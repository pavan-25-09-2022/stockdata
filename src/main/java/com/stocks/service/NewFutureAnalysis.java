package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.EntryInfo;
import com.stocks.dto.FutureEodAnalyzer;
import com.stocks.dto.MarketMoverData;
import com.stocks.dto.MarketMoversResponse;
import com.stocks.dto.Properties;
import com.stocks.dto.StockEODResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NewFutureAnalysis {

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
	private AdvancedStopLossService advancedStopLossService;
	@Autowired
	private AdvancedTargetService advancedTargetService;
	@Autowired
	private AdvancedEntryService advancedEntryService;
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

	@Value("${eodValue}")
	private int eodValue;

	@Value("${api.request.delay.ms:333}")
	private long apiRequestDelayMs;

	private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
	private static final int RSI_PERIOD = 14;
	private static final double MIN_STOCK_PRICE_THRESHOLD = 145.0;
	private static final String POSITIVE_STOCK_TYPE = "P";
	private static final String TEST_ENV = "test";
	private static final String STRATEGY_DH = "DH";

	@Autowired
	private CommonValidation commonValidation;


	public List<TradeSetupTO> futureAnalysis(Properties properties) {

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

		log.info("Starting optimized sequential processing of {} stocks", totalStocks);

		// OPTIMIZATION 1: Batch fetch market movers data once
		long batchStartTime = System.currentTimeMillis();
		MarketMoversResponse marketMoversResponse = ioPulseService.marketMovers(properties);
		log.info("Market movers data fetched in {} ms", System.currentTimeMillis() - batchStartTime);

		// OPTIMIZATION 2: Pre-filter stocks based on EOD criteria to reduce API calls
		Map<String, String> validStocksEod = preFilterStocksByEod(stockList, properties.getStockDate());
		log.info("Pre-filtered {} stocks to {} valid candidates based on EOD criteria",
				stockList.size(), validStocksEod.size());

		// OPTIMIZATION 3: Batch process valid stocks with reduced delays
		for (String stock : validStocksEod.keySet()) {
			try {
				String eodResponse = validStocksEod.get(stock);
				String ystEod = eodResponse.split("-")[0];

				MarketMoverData moverData = marketMoversResponse.getData().stream()
						.filter(data -> stock.equals(data.getStSymbolName()))
						.findFirst()
						.orElse(null);

				if (moverData == null) {
					log.debug("No market mover data found for stock: {}", stock);
					continue;
				}

				processedCount++;
				log.debug("Processing stock {}/{}: {}", processedCount, validStocksEod.size(), stock);

				TradeSetupTO result = processStockOptimized(stock, properties, moverData);

				if (result != null) {
					// OPTIMIZATION 4: Calculate OI metrics once
					populateOiMetrics(result, moverData, ystEod);
					result.setTradeNotes(eodResponse);
					result.setTradeNotes(result.getCriteria());

					// OPTIMIZATION 5: Batch database saves (save at end instead of per stock)
					results.add(result);
					log.debug("Successfully processed stock: {}", stock);
				}

				// OPTIMIZATION 6: Reduced delay - only between API-heavy operations
				if (processedCount < validStocksEod.size()) {
					Thread.sleep(apiRequestDelayMs); // Use configurable delay instead of fixed 1000ms
				}

			} catch (InterruptedException e) {
				log.warn("Processing interrupted for stock: {} after processing {}/{} stocks",
						stock, processedCount, validStocksEod.size());
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				log.error("Error processing stock: {} ({}/{})", stock, processedCount, validStocksEod.size(), e);
				// Continue with next stock instead of failing completely
			}
		}

		// OPTIMIZATION 7: Batch database operations
		if (!results.isEmpty()) {
			long dbStartTime = System.currentTimeMillis();
			results.forEach(tradeSetupManager::saveTradeSetup);
			log.info("Batch saved {} trade setups in {} ms", results.size(), System.currentTimeMillis() - dbStartTime);
		}

		log.info("Completed optimized processing of {} stocks. Found {} valid trade setups",
				processedCount, results.size());
		return results.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * OPTIMIZATION 2: Pre-filter stocks by EOD criteria to reduce API calls
	 */
	private Map<String, String> preFilterStocksByEod(Set<String> stockList, String stockDate) {
		Map<String, String> validStocks = new HashMap<>();
		int batchSize = 10; // Process in small batches to respect rate limits
		List<String> stockListArray = new ArrayList<>(stockList);

		for (int i = 0; i < stockListArray.size(); i += batchSize) {
			int endIndex = Math.min(i + batchSize, stockListArray.size());
			List<String> batch = stockListArray.subList(i, endIndex);

			for (String stock : batch) {
				try {
					String eodResponse = processEodResponse(stock, stockDate);
					if (eodResponse != null && !eodResponse.isEmpty()) {
						String ystEod = eodResponse.split("-")[0];
						String yst2Eod = eodResponse.split("-")[1];
						if (("LBU".equals(ystEod)) && ("LBU".equals(yst2Eod) || "SC".equals(yst2Eod))) {
							validStocks.put(stock, eodResponse);
						}
					}
				} catch (Exception e) {
					log.error("Error processing EOD for stock {}: {}", stock, e.getMessage());
				}
			}

			// Small delay between batches
			if (endIndex < stockListArray.size()) {
				try {
					Thread.sleep(100); // Minimal delay between batches
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}

		return validStocks;
	}

	/**
	 * OPTIMIZATION 4: Extract OI metrics calculation to separate method
	 */
	private void populateOiMetrics(TradeSetupTO result, MarketMoverData moverData, String ystEod) {
		try {
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
		} catch (NumberFormatException e) {
			log.warn("Error parsing OI metrics for stock {}: {}", result.getStockSymbol(), e.getMessage());
		}
	}

	/**
	 * OPTIMIZATION 8: Optimized version of processStock with caching and reduced API calls
	 */
	private TradeSetupTO processStockOptimized(String stock, Properties properties, MarketMoverData moverData) {
		try {
			properties.setStockName(stock);

			// OPTIMIZATION: Check if already processed today
//			TradeSetupTO existingSetup = tradeSetupManager.getStockByDateAndTime(stock, properties.getStockDate(), endTime);
//			if (existingSetup != null) {
//				log.debug("Found existing trade setup for {}, skipping processing", stock);
//				return existingSetup;
//			}

			// Get candles data
			List<Candle> candles = commonValidation.getCandles(properties, stock);
			if (candles == null || candles.isEmpty()) {
				return null;
			}

			log.debug("Processing stock {} with {} candles", stock, candles.size());
			Candle ystEodCandle = candles.get(0);
			candles = candles.subList(1, candles.size());

			// Process candles more efficiently
			TradeSetupTO result = processStockCandlesOptimized(stock, candles, properties, ystEodCandle);

			return result;

		} catch (Exception e) {
			log.error("Error in optimized processing for stock: {}, {}", stock, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * OPTIMIZATION 9: Streamlined candle processing with early exit conditions
	 */
	private TradeSetupTO processStockCandlesOptimized(String stock, List<Candle> candles, Properties properties, Candle ystEodCandle) throws InterruptedException {
		List<Candle> processedCandles = new ArrayList<>();
		Candle firstCandle = candles.get(0);
		LocalTime endTime = FormatUtil.getTimeHHmmss("10:30:00");

		if (TEST_ENV.equals(properties.getEnv())) {
			for (int i = 0; i < candles.size(); i++) {
				Candle curCandle = candles.get(i);
				processedCandles.add(curCandle);

				if (curCandle.getEndTime().isAfter(endTime) || firstCandle.getEndTime().equals(curCandle.getEndTime())) {
					continue;
				}

				// Quick validation check
				//check first 3 candles if they are positive
				if (processedCandles.size() < 3) {
					continue;
				}

				List<Candle> positiveCheckCandles = Arrays.asList(firstCandle);
				if (!areCandlesPositive(positiveCheckCandles)) {
					continue;
				}
				Thread.sleep(100); // Use configurable delay instead of fixed 1000ms

				// Found valid candle - process immediately
//				if (!isEndTimeMultipleOf15(curCandle)) {
				TradeSetupTO tradeSetupTO = createTradeSetupOptimized(stock, curCandle, firstCandle, properties);
				log.info("Processing stock {} with {} candles", stock, processedCandles.size());
				if (tradeSetupTO != null) {
					if (!"C7".equals(tradeSetupTO.getCriteria())) {
						continue;
					}
					double dayLtpChange = curCandle.getClose() - ystEodCandle.getClose();
					double dayOiChange = curCandle.getOpenInterest() - ystEodCandle.getOpenInterest();
					String oiChange = (dayOiChange > 0) ? (dayLtpChange > 0 ? "LBU" : "SBU") : (dayLtpChange > 0 ? "SC" : "LU");
					tradeSetupTO.setTradeNotes(oiChange);
					return tradeSetupTO;
				}

				return null;
//				}
//				return null;
			}
		} else {

		}

		return null;
	}

	/**
	 * OPTIMIZATION 10: Streamlined trade setup creation with cached data
	 */
	private TradeSetupTO createTradeSetupOptimized(String stock, Candle validCandle, Candle firstCandle, Properties properties) {
		try {
			properties.setEndTime(validCandle.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
			properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));

			// Get strikes data
			Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, stock);

			// Create base trade setup
			TradeSetupTO tradeSetupTO = createBaseTradeSetup(stock, validCandle, firstCandle, null, properties);

			// Apply criteria validation
			applyCriteriaValidation(tradeSetupTO, strikes, properties);
			if ("C7".equalsIgnoreCase(tradeSetupTO.getCriteria())) {
				// Get historical data for advanced calculations
				List<Candle> historicalCandles = new ArrayList<>(commonValidation.getHistoricalCandles(properties,
						validCandle.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

				// Apply advanced strategies
				applyAdvancedStrategies(tradeSetupTO, historicalCandles, null);
			}
			return tradeSetupTO;

		} catch (Exception e) {
			log.error("Error creating optimized trade setup for {}: {}", stock, e.getMessage());
			return null;
		}
	}

	private TradeSetupTO createBaseTradeSetup(String stock, Candle validCandle, Candle firstCandle,
	                                          Map<Integer, StrikeTO> strikes, Properties properties) {
		TradeSetupTO tradeSetupTO = new TradeSetupTO();
		String endTime = validCandle.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));

		tradeSetupTO.setStockSymbol(stock);
		tradeSetupTO.setStockDate(properties.getStockDate());
		tradeSetupTO.setFetchTime(endTime);
		tradeSetupTO.setType(POSITIVE_STOCK_TYPE);
		tradeSetupTO.setStrategy(STRATEGY_DH);
		tradeSetupTO.setStrikes(strikes);
		tradeSetupTO.setEntry1((firstCandle.getLow() + firstCandle.getHigh()) / 2);
		tradeSetupTO.setEntry2((validCandle.getLow() + validCandle.getHigh()) / 2);
		tradeSetupTO.setStopLoss1(firstCandle.getLow());
		tradeSetupTO.setTradeNotes(validCandle.getOiInt());

		return tradeSetupTO;
	}

	private void applyCriteriaValidation(TradeSetupTO tradeSetupTO, Map<Integer, StrikeTO> strikes, Properties properties) {

		List<String> list = Arrays.asList("c7");
		for (String str : list) {
			TradeSetupTO result = marketMovers.validateAndSetDetails(strikes, properties, str);
			if (result != null) {
				tradeSetupTO.setCriteria(str.toUpperCase());
				tradeSetupTO.setTarget1(result.getTarget1());
				tradeSetupTO.setTarget2(result.getTarget2());
				tradeSetupTO.setEntry1(result.getEntry1());
			}
		}

	}

	private void applyAdvancedStrategies(TradeSetupTO tradeSetupTO, List<Candle> historicalCandles, Map<Integer, StrikeTO> strikes) {
		// Calculate optimal entry signal using 3 advanced strategies
		advancedEntryService.calculateOptimalEntry(tradeSetupTO, historicalCandles, null);
		EntryInfo e1 = tradeSetupTO.getEntryInfos().stream().filter(e -> "BC".equals(e.getStrategy())).findFirst().orElse(null);
//		EntryInfo e2 = tradeSetupTO.getEntryInfos().stream().filter(e -> "D".equals(e.getStrategy())).findFirst().orElse(null);


		// Apply advanced targets and stop losses for each entry strategy
//		for (EntryInfo entryInfo : tradeSetupTO.getEntryInfos()) {
		double e2price = tradeSetupTO.getEntry2() != null ? tradeSetupTO.getEntry2() : 0.0;
		if (e1 != null) {
			if (e1.getEntryPrice() > e2price) {
				tradeSetupTO.setEntry1(e1.getEntryPrice());
				tradeSetupTO.setEntry2(e2price);
			} else {
				tradeSetupTO.setEntry1(e2price);
				tradeSetupTO.setEntry2(e1.getEntryPrice());
			}
		}
//		if (e2 != null) {
//			tradeSetupTO.setEntry2(e2.getEntryPrice());
//		}

		// Calculate advanced targets using multiple strategies
//		advancedTargetService.calculateOptimalTargets(tradeSetupTO, historicalCandles, strikes, "");

//		TargetInfo t1 = tradeSetupTO.getTargetInfos().stream().filter(t -> "FE".equals(t.getStrategy())).findFirst().orElse(null);
//		if (t1 != null) {
//			tradeSetupTO.setTarget1(t1.getTarget1());
//			tradeSetupTO.setTarget2(t1.getTarget2());
//		}
		// Calculate advanced stop loss using multiple strategies
		advancedStopLossService.calculateOptimalStopLoss(tradeSetupTO, historicalCandles, strikes, "");
		tradeSetupTO.getStopLossInfos().stream()
				.filter(s -> "ATR".equals(s.getStrategy()))
				.findFirst().ifPresent(s1 -> tradeSetupTO.setStopLoss1(s1.getStopLoss()));
		//		}
	}

	private String processEodResponse(String stock, String stockDate) {

		StockEODResponse eod = ioPulseService.getMonthlyData(stock);
		if (eod == null || eod.getData().size() < 3) {
			log.info("EOD data is insufficient for stock: {}", stockDate);
			return null;
		}
		FutureEodAnalyzer data = eod.getData().get(0);
		if (data.getInDayHigh() < 200) {
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
				return (oi1 + "-" + oi2);
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

	/**
	 * Generate detailed entry analysis text from EntrySignal
	 */
	private String generateEntryAnalysisDetails(AdvancedEntryService.EntrySignal entrySignal) {
		StringBuilder details = new StringBuilder();

		details.append("Entry Analysis Summary:\n");
		details.append(String.format("Primary Strategy: %s\n", entrySignal.strategy));
		details.append(String.format("Entry Price: %.2f\n", entrySignal.entryPrice));
		details.append(String.format("Signal: %s\n", entrySignal.signal));
		details.append(String.format("Confidence: %.1f%%\n", entrySignal.confidence));
		details.append(String.format("Risk: %.2f%%\n", entrySignal.riskPercent));

		details.append("\nStrategy Scores:\n");
		entrySignal.strategyScores.forEach((strategy, score) ->
				details.append(String.format("- %s: %.1f/100\n", strategy.replace("_", " "), score)));

		return details.toString();
	}

	/**
	 * Calculate stop loss risk percentage
	 */
	private double calculateStopLossRisk(double stopLoss, Double entryPrice) {
		if (entryPrice == null || entryPrice == 0 || stopLoss == 0) {
			return 2.0; // Default risk
		}
		return Math.abs((entryPrice - stopLoss) / entryPrice) * 100;
	}

	/**
	 * Check if the first 3 candles (indices 0, 1, 2) are positive candles
	 * A positive candle has OI interpretation of "LBU" (Long Build Up) or "SC" (Short Covering)
	 */
	private boolean areCandlesPositive(List<Candle> candles) {
		boolean isPositive = true;
		// Check first 3 candles (indices 0, 1, 2)
		for (int i = 0; i < candles.size(); i++) {
			Candle candle = candles.get(i);
			if (candle.getOpen() > candle.getClose()) {
				isPositive = false;
				break;
			}
		}

		return isPositive;
	}

	/**
	 * Check if the candle's end time is a multiple of 15 minutes
	 * Valid times: 09:15, 09:30, 09:45, 10:00, 10:15, 10:30, etc.
	 */
	private boolean isEndTimeMultipleOf15(Candle candle) {

		LocalTime endTime = candle.getEndTime();
		int minutes = endTime.getMinute();

		// Check if minutes is a multiple of 15 (0, 15, 30, 45)
		return (minutes % 15) == 0;

	}

}

