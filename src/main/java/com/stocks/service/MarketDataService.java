package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.FutureEodAnalyzer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
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

	@Value("${api.Month.url")
	private String apiMonthUrl;

	@Value("${api.auth.token}")
	private String authToken;

	private String endTime = "12:30";

	@Value("${eodValue}")
	private int eodValue;

	private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
	@Autowired
	private CommonValidation commonValidation;


	public List<TradeSetupTO> callApi(Properties properties) {

		// Path to the file containing the stock list
		String filePath = "src/main/resources/stocksList.txt";

		String stockDate = properties.getStockDate();
		LocalDate date = LocalDate.parse(stockDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
		if (isWeekend) {
			log.info("The provided date {} is a weekend. Exiting the process.", stockDate);
			return new ArrayList<>();
		}
		// Read all lines from the file into a List
		Set<String> stockList;
		if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
			stockList = Arrays.stream(properties.getStockName().split(","))
					.map(String::trim)
					.collect(Collectors.toSet());
		} else {
			stockList = ioPulseService.getAvailableStocks();
		}

		ForkJoinPool customThreadPool = new ForkJoinPool(10);
		try {
			return customThreadPool.submit(() ->
					stockList.stream()
							.map(stock -> processStock(stock, properties))
							.filter(Objects::nonNull)
							.distinct()
							.collect(Collectors.toList())
			).get();
		} catch (Exception e) {
			log.error("Error in parallel processing: {}", e.getMessage(), e);
			return new ArrayList<>();
		} finally {
			customThreadPool.shutdown();
		}
	}

	private TradeSetupTO processStock(String stock, Properties properties) {
		try {
			properties.setStockName(stock);
			List<Candle> candles = commonValidation.getCandles(properties, stock);
			if (candles == null || candles.isEmpty()) {
				return null;
			}
			log.info("Processing stock {}", stock);
			Candle ystEodCandle = candles.get(0);
			candles = candles.subList(1, candles.size());
			Properties prop = new Properties();
			prop.setStockDate(FormatUtil.addDays(properties.getStockDate(), -1));
			prop.setStockName(properties.getStockName());
			List<Candle> list = new ArrayList<>();
			StockResponse sr1 = null;
			Candle luCandle = null;
			Candle sbuCandle = null;
			Candle lbuCandle = null;
			List<Long> volumes = new ArrayList<>();
			for (Candle curCandle : candles) {
				list.add(curCandle);
				if (curCandle.getEndTime().isAfter(LocalTime.parse(endTime))) {
					continue; // Skip candles after the specified end time
				}
				volumes.add(curCandle.getVolume());

//				StockResponse res = processCandleSticks.getStockResponse(stock, properties, list, new ArrayList<>());

				List<Double> rsiList = new ArrayList<>();
				Candle firstCandle = candles.get(0);
				rsiList.add(firstCandle.getClose());

				Map<Long, Double> map = new HashMap<>();
				double prevChgeInPer = 0.0;
				StockResponse res1 = null;
				Candle prevCandle = firstCandle;
				double dayHigh = firstCandle.getHigh();
				rsiList.add(curCandle.getClose());
				if (rsiList.size() > 14) {
					rsiList.remove(0); // Remove the oldest element
				}
				LocalTime localTime = curCandle.getStartTime();
				double ltpChange = curCandle.getClose() - prevCandle.getClose();
				ltpChange = Double.parseDouble(String.format("%.2f", ltpChange));
				long oiChange = curCandle.getOpenInterest() - prevCandle.getOpenInterest();
				String oiInterpretation = (oiChange > 0)
						? (ltpChange > 0 ? "LBU" : "SBU")
						: (ltpChange > 0 ? "SC" : "LU");
				double val1 = ((curCandle.getHigh() - curCandle.getLow()) / curCandle.getLow()) * 100;
				double firstCandleChgeInPer = (((firstCandle.getClose() - firstCandle.getOpen()) / firstCandle.getOpen()) * 100);
				double highToOpenChge = (((curCandle.getHigh() - curCandle.getOpen()) / curCandle.getOpen()) * 100);
				double lowToCloseChge = (((curCandle.getClose() - curCandle.getLow()) / curCandle.getLow()) * 100);

//            double value = rsiCalculator.calculateRSI(rsiList, 14);
				StockResponse res = new StockResponse();
				res.setStock(stock);
				res.setOiInterpretation(oiInterpretation);
//            res.setRsi(value);
				res.setCurCandle(curCandle);
				res.setFirstCandle(firstCandle);
//				if (curCandle.isHighVolume()) {
				if (commonValidation.isPositive(candles, firstCandle, curCandle, curCandle.getOiInt()) && curCandle.getHigh() > dayHigh) {
					res.setStockType("P");
					res.setValidCandle(curCandle);
				}
//				}
				dayHigh = Math.max(dayHigh, curCandle.getHigh());
				prevCandle = curCandle;
				if (res.getValidCandle() != null && res.getValidCandle().getHigh() > 145) {
					if ("test".equals(properties.getEnv()) || list.size() == candles.size()) {
						properties.setEndTime(res.getValidCandle().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
						properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));
						Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, properties.getStockName());
						TradeSetupTO tradeSetupTO = marketMovers.validateAndSetDetails(strikes, stock, "c2");
						if (tradeSetupTO == null) {
							continue;
						}
						populateTradeSetup(tradeSetupTO, res, strikes, properties);
						tradeSetupTO.setStopLoss1(ystEodCandle.getLow());

//					if ("1".equals(processEodResponse(tradeSetupTO.getStockSymbol(), "P", res.getValidCandle().getOiInt(), res.getCurHigh(), properties.getStockDate()))) {
						tradeSetupManager.saveTradeSetup(tradeSetupTO);
						return tradeSetupTO;
//					}
					} else {

					}
				}

			}
//			if (sr1 != null && sr1.getStock().contains("*")) {
//				if ("test1".equalsIgnoreCase(properties.getEnv()) && sr1.getStockProfitResult() == null) {
//					log.info("{} first candle {}", sr1.getStock(), sr1.getFirstCandle());
//					log.info("{} current candle {}", sr1.getStock(), sr1.getCurCandle());
//					log.info("{} valid candle {}", sr1.getStock(), sr1.getValidCandle());
//					int i = 1;
//					boolean calculateForward = true;
//					int maxDays = 5;
//					String curDate = FormatUtil.addDays(LocalDate.now().toString(), 1);
//					Candle prevCandle = null;
//					while (calculateForward) {
//						Thread.sleep(2000);
//						Properties prop1 = new Properties();
//						prop1.setStockDate(FormatUtil.addDays(properties.getStockDate(), i));
//						prop1.setStockName(properties.getStockName());
//						if (prop1.getStockDate().equals(curDate)) {
//							calculateForward = false;
//						}
//						try {
//							List<Candle> plus1Candles = commonValidation.getCandles(prop1, stock);
//							log.info("Date {} candles {} curDate {}", prop1.getStockDate(), plus1Candles.size(), curDate);
//							for (Candle c : plus1Candles) {
//								prevCandle = c;
//								commonValidation.checkExitSignal(sr1, c);
//								if (sr1.getStockProfitResult() != null) {
//									String sell = sr1.getStockProfitResult().getSellTime();
//									sr1.getStockProfitResult().setSellTime(sell + " " + prop1.getStockDate());
//								}
//							}
//						} catch (Exception e) {
//							calculateForward = false;
//						}
//						if (sr1.getStockProfitResult() != null) {
//							calculateForward = false;
//						}
//						if (maxDays == i) {
//							calculateForward = false;
//						}
//						i++;
//					}
//
//				}
//
//				return sr1;
//			} else {
//				return sr1;
//			}
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
		tradeSetupTO.setStrategy("DH");
		tradeSetupTO.setStrikes(strikes);
		tradeSetupTO.setEntry1((lastCandle.getLow() + lastCandle.getHigh()) / 2);
//		tradeSetup.setEntry2(res.getFirstCandle().getClose());
		tradeSetupTO.setStopLoss1(firstCandle.getLow());
//		StrikeTO strike = commonUtils.getLargestCeVolumeStrike(strikes);
//		tradeSetup.setTarget1(strike.getStrikePrice());
//		if (strike.getIndex() <= 0 || strike.getPeOiChg() <= 0) {
//			return null;
//		}
//		StrikeTO strike = commonUtils.getTarget2Strike(strikes);
//		if (strike != null) {
//			tradeSetup.setTarget2(res.getFirstCandle().getClose());
//		}
//		return tradeSetupTO;
	}

	private String processEodResponse(String stock, String stockType, String oiInt, double curHigh, String stockDate) {

		StockEODResponse eod = ioPulseService.getMonthlyData(stock);
		if (eod == null || eod.getData().size() < 3) {
			log.info("EOD data is insufficient for stock: {}", stock);
			return null;
		}

		// Extract data for the last three days
		for (int i = 0; i < eod.getData().size(); i++) {
			FutureEodAnalyzer futureEodAnalyzer = eod.getData().get(i);
			if (stockDate.equals(futureEodAnalyzer.getStFetchDate())) {
				FutureEodAnalyzer dayM1 = eod.getData().get(i + 1);
				FutureEodAnalyzer dayM2 = eod.getData().get(i + 2);
				FutureEodAnalyzer dayM3 = eod.getData().get(i + 3);
				String oi1 = calculateOiInterpretation(dayM1, dayM2);
				String oi2 = calculateOiInterpretation(dayM2, dayM3);

				if ("N".equals(stockType) && "SBU".equals(oiInt)) {
				} else if ("P".equals(stockType) && "LBU".equals(oiInt)) {
//					if (curHigh > dayM1.getInDayHigh())
					{
						return determinePriority(oi1, "LBU", "SC");
					}
				}
			}
		}
		return null;
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

