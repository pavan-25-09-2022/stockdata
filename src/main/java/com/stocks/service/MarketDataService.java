package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.FutureEodAnalyzer;
import com.stocks.dto.MarketMoverData;
import com.stocks.dto.MarketMoversResponse;
import com.stocks.dto.Properties;
import com.stocks.dto.StockEODResponse;
import com.stocks.dto.StockResponse;
import com.stocks.dto.StrikeTO;
import com.stocks.dto.TradeSetupTO;
import com.stocks.repository.TradeSetupManager;
import com.stocks.utils.CommonUtils;
import com.stocks.utils.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

	@Autowired
	private IOPulseService ioPulseService;
	@Autowired
	private TestingResultsService testingResultsService;
	@Autowired
	private StockDataManager stockDataManager;
	@Autowired
	private OptionChainService optionChainService;
	@Autowired
	private RSICalculator rsiCalculator;
	@Autowired
	private ProcessCandleSticks processCandleSticks;
	@Autowired
	private CalculateOptionChain calculateOptionChain;
	@Autowired
	private CommonUtils commonUtils;
	@Autowired
	private TradeSetupManager tradeSetupManager;
	@Autowired
	private MarketMovers marketMovers;

	@Value("${api.url}")
	private String apiUrl;

	@Value("${api.auth.token1}")
	private String authToken;

	@Value("${market.end.time:12:30}")
	private String endTime;

	@Value("${eodValue}")
	private int eodValue;

	@Value("${api.request.delay.ms:100}")
	private long apiRequestDelayMs;

	private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
	private static final int RSI_PERIOD = 14;
	private static final double MIN_STOCK_PRICE_THRESHOLD = 145.0;
	private static final String POSITIVE_STOCK_TYPE = "P";
	private static final String TEST_ENV = "test";
	private static final String STRATEGY_DH = "DH";

	@Autowired
	private CommonValidation commonValidation;


	public List<TradeSetupTO> callApi(Properties properties) {
		if (properties == null) {
			log.error("Properties cannot be null");
			return new ArrayList<>();
		}

		if (properties.getStockDate() == null || properties.getStockDate().trim().isEmpty()) {
			log.error("Stock date cannot be null or empty");
			return new ArrayList<>();
		}

		if (!isValidTradingDay(properties.getStockDate())) {
			return new ArrayList<>();
		}

		Set<String> stockList = getStockList(properties);
		if (stockList.isEmpty()) {
			log.warn("No stocks to process");
			return new ArrayList<>();
		}

		return processStocksSequentially(stockList, properties);
	}

	private boolean isValidTradingDay(String stockDate) {
		try {
			LocalDate date = LocalDate.parse(stockDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
			if (isWeekend) {
				log.info("The provided date {} is a weekend. Exiting the process.", stockDate);
				return false;
			}
			return true;
		} catch (Exception e) {
			log.error("Invalid date format: {}. Expected format: yyyy-MM-dd", stockDate, e);
			return false;
		}
	}

	private Set<String> getStockList(Properties properties) {
		if (properties.getStockName() != null && !properties.getStockName().trim().isEmpty()) {
			return Arrays.stream(properties.getStockName().split(","))
					.map(String::trim)
					.filter(stock -> !stock.isEmpty())
					.collect(Collectors.toSet());
		} else {
			try {
				Set<String> availableStocks = ioPulseService.getAvailableStocks();
				return availableStocks != null ? availableStocks : new HashSet<>();
			} catch (Exception e) {
				log.error("Error fetching available stocks", e);
				return new HashSet<>();
			}
		}
	}

	private List<TradeSetupTO> processStocksSequentially(Set<String> stockList, Properties properties) {
		List<TradeSetupTO> results = new ArrayList<>();
		int totalStocks = stockList.size();
		int processedCount = 0;

		log.info("Starting sequential processing of {} stocks with {}ms delay between requests", totalStocks, apiRequestDelayMs);
		MarketMoversResponse marketMoversResponse = ioPulseService.marketMovers(properties);
		for (String stock : stockList) {
			try {
				MarketMoverData moverData = marketMoversResponse.getData().stream()
						.filter(data -> stock.equals(data.getStSymbolName()))
						.findFirst()
						.orElse(null);
				processedCount++;
				log.debug("Processing stock {}/{}: {}", processedCount, totalStocks, stock);

				TradeSetupTO result = processStock(stock, properties);

				if (result != null) {
					double oldOi = Double.parseDouble(moverData.getInOldOi());
					double newOi = Double.parseDouble(moverData.getInNewOi());
					double oldClose = Double.parseDouble(moverData.getInOldClose());
					double newClose = Double.parseDouble(moverData.getInNewClose());
					double ltpChg = newClose - oldClose;
					double lptChgPer = (ltpChg / oldClose) * 100;
					double oiChg = ((newOi - oldOi) / oldOi) * 100;
					String oiInterpretation = (oiChg > 0)
							? (ltpChg > 0 ? "LBU" : "SBU")
							: (ltpChg > 0 ? "SC" : "LU");
					result.setOiChgPer(oiChg);
					result.setLtpChgPer(lptChgPer);
					result.setTradeNotes(oiInterpretation);
					tradeSetupManager.saveTradeSetup(result);
					results.add(result);
					log.debug("Successfully processed stock: {}", stock);
				}

				// Add configurable delay to avoid overwhelming the external API
				// Reduce delay for test environment
				//long actualDelay = TEST_ENV.equals(properties.getEnv()) ? Math.min(apiRequestDelayMs, 200) : apiRequestDelayMs;
				//if (processedCount < totalStocks && actualDelay > 0) {
				Thread.sleep(1000);
				//}

			} catch (InterruptedException e) {
				log.warn("Processing interrupted for stock: {} after processing {}/{} stocks", stock, processedCount, totalStocks);
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				log.error("Error processing stock: {} ({}/{})", stock, processedCount, totalStocks, e);
				// Continue with next stock instead of failing completely
			}
		}

		log.info("Completed processing {} stocks. Found {} valid trade setups", processedCount, results.size());
		return results.stream().distinct().collect(Collectors.toList());
	}

	private TradeSetupTO processStock(String stock, Properties properties) {
		try {
			properties.setStockName(stock);
			List<Candle> candles = commonValidation.getCandles(properties, stock);
			if (candles == null || candles.isEmpty()) {
				return null;
			}
			log.debug("Processing stock {}", stock);
			Candle ystEodCandle = candles.get(0);
			candles = candles.subList(1, candles.size());
			List<Candle> processedCandles = new ArrayList<>();
			List<Long> volumes = new ArrayList<>();
			for (Candle curCandle : candles) {
				processedCandles.add(curCandle);
				if (curCandle.getEndTime().isAfter(LocalTime.parse(endTime))) {
					continue; // Skip candles after the specified end time
				}
				volumes.add(curCandle.getVolume());


				List<Double> rsiList = new ArrayList<>();
				Candle firstCandle = candles.get(0);
				rsiList.add(firstCandle.getClose());

				Candle prevCandle = firstCandle;
				double dayHigh = firstCandle.getHigh();
				rsiList.add(curCandle.getClose());
//				if (rsiList.size() > RSI_PERIOD) {
//					rsiList.remove(0); // Remove the oldest element
//				}
				double ltpChange = Math.round((curCandle.getClose() - prevCandle.getClose()) * 100.0) / 100.0;
				long oiChange = curCandle.getOpenInterest() - prevCandle.getOpenInterest();
				String oiInterpretation = (oiChange > 0)
						? (ltpChange > 0 ? "LBU" : "SBU")
						: (ltpChange > 0 ? "SC" : "LU");
				TradeSetupTO tradeSetupTO1 = tradeSetupManager.getStockByDateAndTime(stock, properties.getStockDate(), endTime);
				if (tradeSetupTO1 != null) {
					return tradeSetupTO1;
				}
				StockResponse res = new StockResponse();
				res.setStock(stock);
				res.setOiInterpretation(oiInterpretation);
				res.setCurCandle(curCandle);
				res.setFirstCandle(firstCandle);
				if (commonValidation.isPositive(candles, firstCandle, curCandle, curCandle.getOiInt()) && curCandle.getHigh() > dayHigh) {
					res.setStockType(POSITIVE_STOCK_TYPE);
					res.setValidCandle(curCandle);
				}
				dayHigh = Math.max(dayHigh, curCandle.getHigh());
				prevCandle = curCandle;
				if (res.getValidCandle() != null && res.getValidCandle().getHigh() > MIN_STOCK_PRICE_THRESHOLD) {
					if (TEST_ENV.equals(properties.getEnv()) || processedCandles.size() == candles.size()) {
						properties.setEndTime(res.getValidCandle().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
						properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));
						Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, properties.getStockName());
						TradeSetupTO tradeSetupTO = marketMovers.validateAndSetDetails(strikes, stock, "c2");
						if (tradeSetupTO == null) {
							continue;
						}
						populateTradeSetup(tradeSetupTO, res, strikes, properties);
						if (ystEodCandle.getHigh() < res.getValidCandle().getHigh()) {
							tradeSetupTO.setTradeNotes("Y");
						} else {
							tradeSetupTO.setTradeNotes("N");
						}
						tradeSetupTO.setStopLoss1(ystEodCandle.getLow());
						tradeSetupTO.setTradeNotes(res.getValidCandle().getOiInt());
						processEodResponse(tradeSetupTO);
						return tradeSetupTO;
					}
				}

			}
		} catch (Exception e) {
			log.error("Error processing stock: {}, {}", stock, e.getMessage(), e);
		}
		return null;
	}

	private void populateTradeSetup(TradeSetupTO tradeSetupTO, StockResponse res, Map<Integer, StrikeTO> strikes, Properties properties) {
		String endTime = res.getValidCandle().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
		String startTime = res.getValidCandle().getEndTime().plusMinutes(-1).format(DateTimeFormatter.ofPattern("HH:mm"));

		List<Candle> candles = commonValidation.getCandles(properties, properties.getStockDate() + " 09:15", properties.getStockDate() + " " + startTime);
		Candle lastCandle = candles.get(0);
		Candle firstCandle = candles.get(candles.size() - 1);
		tradeSetupTO.setStockSymbol(res.getStock());
		tradeSetupTO.setStockDate(properties.getStockDate());
		tradeSetupTO.setFetchTime(endTime);
		tradeSetupTO.setType(res.getStockType());
		tradeSetupTO.setStrategy(STRATEGY_DH);
		tradeSetupTO.setStrikes(strikes);
		tradeSetupTO.setEntry1((lastCandle.getLow() + lastCandle.getHigh()) / 2);
		tradeSetupTO.setStopLoss1(firstCandle.getLow());
	}

	private void processEodResponse(TradeSetupTO tradeSetupTO) {

		StockEODResponse eod = ioPulseService.getMonthlyData(tradeSetupTO.getStockSymbol());
		if (eod == null || eod.getData().size() < 3) {
			log.info("EOD data is insufficient for stock: {}", tradeSetupTO.getStockDate());
			return;
		}

		// Extract data for the last three days
		for (int i = 0; i < eod.getData().size(); i++) {
			FutureEodAnalyzer futureEodAnalyzer = eod.getData().get(i);
			if (tradeSetupTO.getStockDate().equals(futureEodAnalyzer.getStFetchDate())) {
				FutureEodAnalyzer dayM1 = eod.getData().get(i + 1);
				FutureEodAnalyzer dayM2 = eod.getData().get(i + 2);
				FutureEodAnalyzer dayM3 = eod.getData().get(i + 3);
				String oi1 = calculateOiInterpretation(dayM1, dayM2);
				String oi2 = calculateOiInterpretation(dayM2, dayM3);
				tradeSetupTO.setTradeNotes(oi1 + "-" + oi2);
				return;
			}
		}
	}

	private String calculateOiInterpretation(FutureEodAnalyzer current, FutureEodAnalyzer previous) {
		double ltpChange = Double.parseDouble(String.format("%.2f", current.getInClose() - previous.getInClose()));
		long oiChange = Long.parseLong(current.getInOi()) - Long.parseLong(previous.getInOi());
		return (oiChange > 0)
				? (ltpChange > 0 ? "LBU" : "SBU")
				: (ltpChange > 0 ? "SC" : "LU");
	}

	private String determinePriority(String oi, String primary, String secondary) {
		if (primary.equals(oi)) {
			return "1";
		} else if (secondary.equals(oi)) {
			return "1";
		}
		return "0"; // Default priority if no match
	}

}

