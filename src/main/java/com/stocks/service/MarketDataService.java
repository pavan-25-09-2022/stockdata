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
//			List<Candle> candles = commonValidation.getCandles(properties, properties.getStockDate() + " 09:15", properties.getStockDate() + " 03:15");
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
//			List<Candle> ystCandles = commonValidation.getCandles(properties, prop.getStockDate() + " 09:15", prop.getStockDate() + " 03:15");
//            List<Candle> ystCandles = commonValidation.getCandles(prop, stock);
			List<Candle> list = new ArrayList<>();
			StockResponse sr1 = null;
			Candle luCandle = null;
			Candle sbuCandle = null;
			Candle lbuCandle = null;
			List<Long> volumes = new ArrayList<>();
			for (Candle c : candles) {
				list.add(c);
//				ystCandles.add(c);
				if (list.size() < 2) {
					continue; // Skip if not enough candles
				}
				if (c.getEndTime().isAfter(LocalTime.parse(endTime))) {
					continue; // Skip candles after the specified end time
				}
//				if (sr1 != null) {
//					lbuCandle = sr1.getValidCandle();
//					if (lbuCandle != null && "LBU".equals(c.getOiInt())) {
//						if (lbuCandle.getVolume() < c.getVolume()) {
//							lbuCandle = c;
//						}
//					}
//                    else if (lbuCandle != null && "LU".equals(c.getOiInt())) {
//                        if (luCandle == null) {
//                            luCandle = c;
//                        } else if (Math.abs(luCandle.getLtpChange()) > Math.abs(c.getLtpChange())) {
//                            luCandle = c;
//                        }
//                    }
//					else if (lbuCandle != null && "SBU".equals(c.getOiInt())) {
//						if (sbuCandle == null) {
//							sbuCandle = c;
//						} else if (Math.abs(c.getCandleStrength()) > Math.abs(lbuCandle.getCandleStrength())) {
//							sbuCandle = c;
//						}
//					}
//
//				}
				volumes.add(c.getVolume());
//				if (sr1 != null) {
//					Candle validCandle = sr1.getValidCandle();
//					if ("test1".equalsIgnoreCase(properties.getEnv()) && sr1.getStock().contains("*")) {
//						commonValidation.checkExitSignal(sr1, c);
//					}
//					if (c.getVolume() > 1000 && validCandle != null && c.getEndTime().isBefore(LocalTime.of(10, 30)) && c.getOpen() <= validCandle.getLow() && c.getClose() > c.getOpen() && (c.getOiInt().equals("LBU") || c.getOiInt().equals("SC"))) {
//						if (lbuCandle != null) {
//							if (sbuCandle != null && Math.abs(sbuCandle.getLtpChange()) > Math.abs(lbuCandle.getLtpChange())) {
////                            if (((sbuCandle != null && sbuCandle.getVolume() >= 0.9 * lbuCandle.getVolume() && Math.abs(sbuCandle.getLtpChange()) > Math.abs(lbuCandle.getLtpChange())) || (luCandle != null && luCandle.getVolume() >= 0.9 * lbuCandle.getVolume()) && luCandle.getLtpChange() > lbuCandle.getLtpChange())) {
//								continue;
//							}
//						}
//
//						sr1.setStock(stock + "*");
//						sr1.setCurCandle(c);
//					}
//				} else
				{
					StockResponse res = processCandleSticks.getStockResponse(stock, properties, list, new ArrayList<>());

					if (res != null && res.getValidCandle() != null && res.getValidCandle().getHigh() > 100) {
//                if (Math.abs(res.getChgeInPer()) > 1) {
//                    return null;
//                }
						if (ystEodCandle.getHigh() > res.getValidCandle().getHigh()) {
							continue;
						}
						properties.setEndTime(res.getValidCandle().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
						properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));
						Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, properties.getStockName());
						if (strikes == null || strikes.size() < 2) {
							return null;
						}
						if ((strikes.get(1).getStrikePrice() - strikes.get(0).getStrikePrice()) < 5) {
							return null;
						}
						if (strikes.get(0).getPeOiChg() < strikes.get(0).getCeOiChg() || strikes.get(0).getPeVolume() == 0 || strikes.get(0).getCeVolume() == 0) {
							return null;
						}
						TradeSetupTO tradeSetupTO = populateTradeSetup(res, strikes, properties);
						if (tradeSetupTO == null) {
							return null;
						}
						tradeSetupTO.setStopLoss1(ystEodCandle.getLow());
						if (tradeSetupTO.getTarget1() < tradeSetupTO.getEntry1()) {
							log.info("Target1 {} is less than Entry1 {} for stock {}", tradeSetupTO.getTarget1(), tradeSetupTO.getEntry1(), stock);
							return null;
						}
						if (tradeSetupTO.getTarget1() < 500) {
							if (tradeSetupTO.getTarget1() < tradeSetupTO.getEntry1() + 2) {
								return null;
							}
						} else if (tradeSetupTO.getTarget1() > 1500) {
							if (tradeSetupTO.getTarget1() < tradeSetupTO.getEntry1() + 10) {
								return null;
							}
						} else {
							if (tradeSetupTO.getTarget1() < tradeSetupTO.getEntry1() + 4) {
								return null;
							}
						}
						if (ystEodCandle.getHigh() < res.getValidCandle().getHigh()) {
//							tradeSetupTO.setTradeNotes("Y EOD H-B");
							tradeSetupManager.saveTradeSetup(tradeSetupTO);
							return tradeSetupTO;
						}

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

	private TradeSetupTO populateTradeSetup(StockResponse res, Map<Integer, StrikeTO> strikes, Properties properties) {
		String endTime = res.getValidCandle().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
		String startTime = res.getValidCandle().getEndTime().plusMinutes(-1).format(DateTimeFormatter.ofPattern("HH:mm"));

		List<Candle> candles = commonValidation.getCandles(properties, properties.getStockDate() + " 09:15", properties.getStockDate() + " " + startTime);
		Candle lastCandle = candles.get(0);
		Candle firstCandle = candles.get(candles.size() - 1);
		TradeSetupTO tradeSetup = new TradeSetupTO();
		tradeSetup.setStockSymbol(res.getStock());
		tradeSetup.setStockDate(properties.getStockDate());
		tradeSetup.setFetchTime(endTime);
		tradeSetup.setType(res.getStockType());
		tradeSetup.setStrategy("DH");
		tradeSetup.setStrikes(strikes);
		tradeSetup.setEntry1((lastCandle.getLow() + lastCandle.getHigh()) / 2);
//		tradeSetup.setEntry2(res.getFirstCandle().getClose());
		tradeSetup.setStopLoss1(firstCandle.getLow());
		StrikeTO strike = commonUtils.getLargestCeVolumeStrike(strikes);
		tradeSetup.setTarget1(strike.getStrikePrice());
		StrikeTO curStrike = strikes.get(0);
		if (strike.getIndex() <= 0 || strike.getPeOiChg() <= 0) {
			return null;
		}
//		StrikeTO strike = commonUtils.getTarget2Strike(strikes);
//		if (strike != null) {
//			tradeSetup.setTarget2(res.getFirstCandle().getClose());
//		}
		if (lastCandle.getClose() > firstCandle.getClose() && lastCandle.getOpen() > firstCandle.getOpen() && curStrike.getCeOiChg() < 0 && curStrike.getPeOiChg() > 0) {
			return tradeSetup;
		} else {
			return null;
		}
	}

	private void processEodResponse(StockResponse res) {
		if (res == null) {
			log.warn("StockResponse is null, skipping EOD processing.");
			return;
		}

		StockEODResponse eod = ioPulseService.getMonthlyData(res.getStock());
		if (eod == null || eod.getData().size() < 3) {
			log.info("EOD data is insufficient for stock: {}", res.getStock());
			return;
		}

		// Extract data for the last three days
		FutureEodAnalyzer dayM1 = eod.getData().get(eodValue);
		FutureEodAnalyzer dayM2 = eod.getData().get(eodValue + 1);
		FutureEodAnalyzer dayM3 = eod.getData().get(eodValue + 2);

		// Calculate changes and interpretations
		String oi1 = calculateOiInterpretation(dayM1, dayM2);
		String oi2 = calculateOiInterpretation(dayM2, dayM3);
		res.setEodData(oi1 + ", " + oi2);

		// Determine priority based on stock type and interpretations
		if ("N".equals(res.getStockType()) && "SBU".equals(res.getOiInterpretation())) {
//            if (res.getCurLow() < dayM1.getInDayLow()) {
//                res.setYestDayBreak("Y");
//            }
//            res.setPriority(determinePriority(oi1, "SBU", "LU"));
		} else if ("P".equals(res.getStockType()) && "LBU".equals(res.getOiInterpretation())) {
			if (res.getCurHigh() > dayM1.getInDayHigh()) {
				res.setYestDayBreak("Y");
			}
			res.setPriority(determinePriority(oi1, "LBU", "SC"));
		}
	}

	private String calculateOiInterpretation(FutureEodAnalyzer current, FutureEodAnalyzer previous) {
		double ltpChange = Double.parseDouble(String.format("%.2f", current.getInClose() - previous.getInClose()));
		long oiChange = Long.parseLong(current.getInOi()) - Long.parseLong(previous.getInOi());
		return (oiChange > 0)
				? (ltpChange > 0 ? "LBU" : "SBU")
				: (ltpChange > 0 ? "SC" : "LU");
	}

	private int determinePriority(String oi, String primary, String secondary) {
		if (primary.equals(oi)) {
			return 1;
		} else if (secondary.equals(oi)) {
			return 2;
		}
		return 0; // Default priority if no match
	}

}

