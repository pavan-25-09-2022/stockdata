package com.stocks.controller;

import com.stocks.dto.HistoricalQuote;
import com.stocks.dto.Properties;
import com.stocks.dto.StockData;
import com.stocks.dto.StockProfitLossResult;
import com.stocks.dto.StockResponse;
import com.stocks.dto.TradeSetupTO;
import com.stocks.entity.TradeSetupEntity;
import com.stocks.enumaration.QueryInterval;
import com.stocks.mail.Mail;
import com.stocks.mail.MarketMoversMailService;
import com.stocks.repository.TradeSetupManager;
import com.stocks.service.DayHighLowService;
import com.stocks.service.FutureAnalysisService;
import com.stocks.service.FutureEodAnalyzerService;
import com.stocks.service.MarketDataService;
import com.stocks.service.MarketMovers;
import com.stocks.service.OpenHighLowService;
import com.stocks.service.OptionChainService;
import com.stocks.service.RangeBreakoutStrategy;
import com.stocks.service.StockDataManager;
import com.stocks.service.StockResultManager;
import com.stocks.service.TodayFirstCandleTrendLine;
import com.stocks.yahoo.HistQuotesQuery2V8RequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

@RestController
public class ApiController {

	private static final Logger log = LoggerFactory.getLogger(ApiController.class);
	@Autowired
	private MarketDataService apiService;

	@Autowired
	private OptionChainService optionCHainService;

	@Autowired
	private DayHighLowService dayHighLowService;

	@Autowired
	private RangeBreakoutStrategy rangeBreakoutStrategy;

	@Autowired
	private FutureAnalysisService futureAnalysisService;

	@Autowired
	private FutureEodAnalyzerService futureEodAnalyzerService;

	@Autowired
	OpenHighLowService openHighLowService;
	@Autowired
	private TodayFirstCandleTrendLine todayFirstCandleTrendLine;

	@Autowired
	private Mail mailService;
	@Autowired
	private MarketMovers marketMovers;

	@Autowired
	StockDataManager stockDataManager;

	@Autowired
	TradeSetupManager tradeSetupManager;

	@Autowired
	StockResultManager stockResultManager;
	@Autowired
	MarketMoversMailService marketMoversMailService;

	List<String> sectorList = Arrays.asList("NIFTY", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SMALLCAP", "NIFTYIT", "NIFTYFMCG", "NIFTYMETAL", "NIFTYPHARMA");

	@GetMapping("/call-api")
	public String callApi(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                      @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                      @RequestParam(name = "fetchAll", required = false) boolean fetchAll,
	                      @RequestParam(name = "expiryDate", required = false, defaultValue = "") String expiryDate,
	                      @RequestParam(name = "env", required = false, defaultValue = "") String env,
	                      @RequestParam(name = "exitMins", required = false, defaultValue = "0") int exitMins,
	                      @RequestParam(name = "amtInvested", required = false, defaultValue = "0") int amtInvested,
	                      @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setExitMins(exitMins);
		properties.setFetchAll(fetchAll);
		properties.setInterval(interval);
		properties.setAmtInvested(amtInvested);
		properties.setStockName(stockName);
		properties.setExpiryDate(expiryDate);
		properties.setEnv(env);
		List<StockResponse> list1 = apiService.callApi(properties);

		String data = "";
		try {
			if (!list1.isEmpty()) {
				data = mailService.beautifyResults(list1, properties);
//                mailService.sendMail(data, properties);
			}
		} catch (Exception e) {
			log.error("error in beautifyResults: ", e);
		}

		System.gc();
		return data;
	}

	@GetMapping("/apiDayHighLow")
	public String apiDayHighLow(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                            @RequestParam(name = "fetchAll", required = false) boolean fetchAll,
	                            @RequestParam(name = "checkRecentCandle", required = false) boolean checkRecentCandle,
	                            @RequestParam(name = "exitMins", required = false, defaultValue = "0") int exitMins) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setExitMins(exitMins);
		properties.setFetchAll(fetchAll);
		properties.setCheckRecentCandle(checkRecentCandle);
		List<StockResponse> list = dayHighLowService.dayHighLow(properties);
		if (list == null || list.isEmpty()) {
			return "No data found";
		}
		String data = mailService.beautifyResults(list, properties);
		mailService.sendMail(data, properties);
		System.gc();
		return data;
	}

	@GetMapping("/call-option-chain")
	public String callOptionData(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                             @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                             @RequestParam(name = "startTime", required = false, defaultValue = "") String startTime,
	                             @RequestParam(name = "expiryDate", required = false, defaultValue = "") String expiryDate,
	                             @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		properties.setStartTime(startTime);
		properties.setExpiryDate(expiryDate);
		List<StockResponse> list = optionCHainService.getOptionChain(properties);
		if (list == null || list.isEmpty()) {
			return "No data found";
		}
		String data = mailService.beautifyOptChnResults(list);
//        mailService.sendMail(data, properties);
		System.gc();
		return data;
	}

	@GetMapping("/sector")
	public String sector(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                     @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                     @RequestParam(name = "fetchAll", required = false) boolean fetchAll,
	                     @RequestParam(name = "exitMins", required = false, defaultValue = "5") int exitMins) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setExitMins(exitMins);
		properties.setFetchAll(fetchAll);
		properties.setInterval(interval);
		futureEodAnalyzerService.getTrendLinesForNiftyAndBankNifty(properties);
		return "success";
	}

	@GetMapping("/oiChange")
	public List<String> sector(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                           @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		List<String> oiChange = futureEodAnalyzerService.getOIChange(properties);
		return oiChange;
	}

	@GetMapping("/oiChangeForSector")
	public List<String> oiChangeForSector(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                                      @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		List<String> oiChange = futureEodAnalyzerService.getOIChangeForSector(properties);
		return oiChange;
	}


	@GetMapping("/eodAnalyzer")
	public String eodAnalyzer(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                          @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		if (interval < 5) {
			properties.setInterval(5);
		}
		return futureEodAnalyzerService.processEodResponse(properties);
	}

	@GetMapping("/notDayHighLow")
	public List<String> eodAnalyzer(@RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval) {
		Properties properties = new Properties();
		return openHighLowService.dayHighLow(properties);
	}

	@GetMapping("/market-movers")
	public List<String> marketMovers(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		return openHighLowService.dayHighLow(properties);
	}

	@GetMapping("/day-trend-line")
	public String dayTrendLine(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                           @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                           @RequestParam(name = "fetchAll", required = false) boolean fetchAll,
	                           @RequestParam(name = "expiryDate", required = false, defaultValue = "") String expiryDate,
	                           @RequestParam(name = "env", required = false, defaultValue = "") String env,
	                           @RequestParam(name = "exitMins", required = false, defaultValue = "0") int exitMins,
	                           @RequestParam(name = "amtInvested", required = false, defaultValue = "0") int amtInvested,
	                           @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setExitMins(exitMins);
		properties.setFetchAll(fetchAll);
		properties.setInterval(interval);
		properties.setAmtInvested(amtInvested);
		properties.setStockName(stockName);
		properties.setExpiryDate(expiryDate);
		properties.setEnv(env);

		List<StockResponse> list1 = todayFirstCandleTrendLine.getTrendLines(properties);

		String data = "";
		try {
			if (!list1.isEmpty()) {
				data = mailService.beautifyResults(list1, properties);
				mailService.sendMail(data, properties);
			}
		} catch (Exception e) {
			log.error("error in beautifyResults: ", e);
		}

		System.gc();
		return data;
	}

	@GetMapping("/range-break-out")
	public String rangeBreakOut(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                            @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                            @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		rangeBreakoutStrategy.breakOutStocks(properties);

		return "success";
	}

	@GetMapping("/divergenceBasedOnOpenInterest")
	public String divergenceBasedOnOpenInterest(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                                            @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                                            @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName,
	                                            @RequestParam(name = "fileName", required = false) String fileName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		properties.setFileName(fileName);
		return futureEodAnalyzerService.findDivergenceBasedOnOpenInterest(properties);
	}

	@GetMapping("/market-movers-gainers")
	public String marketMoversGainers(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                                  @RequestParam(name = "env", required = false, defaultValue = "") String env,
	                                  @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                                  @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		properties.setEnv(env);
		List<TradeSetupTO> trades = marketMovers.marketMoverDetails(properties, "G");
		String data = marketMoversMailService.beautifyResults(trades);
		marketMoversMailService.sendMail(data, properties);
		return data;
	}

	@GetMapping("market-movers-losers")
	public String marketMoversLosers(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                                 @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                                 @RequestParam(name = "env", required = false, defaultValue = "") String env,
	                                 @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		properties.setEnv(env);
		List<TradeSetupTO> trades = marketMovers.marketMoverDetails(properties, "L");
		String data = marketMoversMailService.beautifyResults(trades);
		marketMoversMailService.sendMail(data, properties);
		return data;
	}

	@GetMapping("/analyseEodResponse")
	public List<String> analyseEodResponse(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                                       @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                                       @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName,
	                                       @RequestParam(name = "fileName", required = false) String fileName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		properties.setFileName(fileName);
		return futureEodAnalyzerService.analyseEodResponse(properties);
	}

	@GetMapping("/getAllRecord")
	public List<StockData> analyseEodResponse(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate) {
		if (stockDate != null && !stockDate.isEmpty()) {
			List<StockData> stockDataList = stockDataManager.getStocksByDate(stockDate);
			if (stockDataList != null && !stockDataList.isEmpty()) {
				return stockDataList;
			} else {
				return Collections.emptyList();
			}
		}
		return stockDataManager.getAllRecord();
	}

	@GetMapping("/getAllTradeSetupRecord")
	public String getAllTradeSetupRecord(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate) {
		List<TradeSetupEntity> allByStockDate = null;
		if (stockDate != null && !stockDate.isEmpty()) {
			allByStockDate = tradeSetupManager.findAllByStockDate(stockDate);
		}else {
			allByStockDate = tradeSetupManager.findAllTradeSetups();
		}
		String data = marketMoversMailService.beautifyTradeSetupResults(allByStockDate);
		//marketMoversMailService.sendMail(data, properties);
		return data;
	}


	@GetMapping("/getAllStrikeSetupRecords")
	public String getAllStrikeSetupRecords(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate) {
		List<TradeSetupEntity> allByStockDate = null;
		if (stockDate != null && !stockDate.isEmpty()) {
			allByStockDate = tradeSetupManager.findAllByStockDate(stockDate);
		}else {
			allByStockDate = tradeSetupManager.findAllTradeSetups();
		}
		StringBuilder sb = new StringBuilder();

		for(TradeSetupEntity tradeSetup : allByStockDate) {
			sb.append("Trade Setup for Stock: ").append(tradeSetup.getStockSymbol()).append(", Date: ").append(tradeSetup.getStockDate()).append("\n");
			if (tradeSetup.getStrikeSetups() != null && !tradeSetup.getStrikeSetups().isEmpty()) {
				sb.append(marketMoversMailService.beautifyStrikeSetupResults(tradeSetup.getStrikeSetups()));
			}
		}
		//marketMoversMailService.sendMail(data, properties);
		return sb.toString();
	}

	@GetMapping("/verifyStockData")
	public List<String> verifyStockData(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                                    @RequestParam(name = "interval", required = false, defaultValue = "5") Integer interval,
	                                    @RequestParam(name = "isAverageAsStopLoss", required = false, defaultValue = "false") boolean isAverageAsStopLoss) {
		stockDate = stockDate.isEmpty() ? LocalDate.now().toString() : stockDate;
		List<StockData> stockDataList = stockDataManager.getStocksByDate(stockDate);
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);

		List<String> stockNames = new ArrayList<>();
		for (StockData data : stockDataList) {
			String[] splitTime = data.getTime().split(":");
			String[] splitDate = data.getDate().split("-");

			Calendar from = Calendar.getInstance();
			from.set(Calendar.YEAR, Integer.parseInt(splitDate[0]));
			from.set(Calendar.MONTH, Integer.parseInt(splitDate[1]) - 1); // Month is 0-based
			from.set(Calendar.DAY_OF_MONTH, Integer.parseInt(splitDate[2]));
			from.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTime[0]));
			from.set(Calendar.MINUTE, Integer.parseInt(splitTime[1]));
			from.set(Calendar.SECOND, 0);
			from.set(Calendar.MILLISECOND, 0);

			Calendar to = Calendar.getInstance();
			to.set(Calendar.YEAR, Integer.parseInt(splitDate[0]));
			to.set(Calendar.MONTH, Integer.parseInt(splitDate[1]) - 1); // Month is 0-based
			to.set(Calendar.DAY_OF_MONTH, Integer.parseInt(splitDate[2]));
			int addedDays = 0;
			while (addedDays < 10) {
				to.add(Calendar.DAY_OF_MONTH, 1);
				int dayOfWeek = to.get(Calendar.DAY_OF_WEEK);
				if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
					addedDays++;
				}
			}
			to.set(Calendar.HOUR_OF_DAY, 15);
			to.set(Calendar.MINUTE, 30);
			to.set(Calendar.SECOND, 0);
			to.set(Calendar.MILLISECOND, 0);

			if (sectorList.contains(data.getStock())) {
				log.info("Skipping sector stock: " + data.getStock());
				continue; // Skip sector stocks
			}

			String symbol = data.getStock() + ".NS"; // For NSE stocks
			QueryInterval queryInterval = QueryInterval.getInstance(interval + "m");
			HistQuotesQuery2V8RequestImpl impl1 = new HistQuotesQuery2V8RequestImpl(symbol, from, to, queryInterval);

			try {
				boolean isEntered = false;
				boolean isTargetAchieved = false;
				boolean isStopLossHit = false;
				boolean isAveragePriceHit = false;
				StockProfitLossResult stockProfitLossResult = null;
				List<HistoricalQuote> completeResult = impl1.getCompleteResult();
				StringBuilder sb = new StringBuilder();
				if (data.getType().equals("Positive")) {

					for (int i = 0; i < completeResult.size() - 1; i++) {

						HistoricalQuote quote = completeResult.get(i);
						if (quote.getHigh() == null || quote.getLow() == null || quote.getHigh().intValue() == 0 || quote.getLow().intValue() == 0) {
							continue; // Skip quotes with null values
						}
						if (!isEntered && (quote.getLow().intValue() < data.getEntryPrice1() || quote.getLow().intValue() < data.getEntryPrice2())) {
							stockProfitLossResult = new StockProfitLossResult();
							stockProfitLossResult.setStock(data.getStock());
							stockProfitLossResult.setDate(data.getDate());
							stockProfitLossResult.setType("Positive");
							stockProfitLossResult.setBuyPrice(quote.getLow().intValue());
							int quantity = (int) (100000 / stockProfitLossResult.getBuyPrice());
							stockProfitLossResult.setQuantity(quantity);
							stockProfitLossResult.setBuyTime(LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()));
							sb.append("Positive Stock ").append(data.getStock()).append("  Entered at: ").append(quote.getDate().getTime()).append(" with price: ").append(quote.getLow().intValue());
							isEntered = true;
						}

                       /* if (!isAveragePriceHit && quote.getLow().intValue() < data.getAveragePrice()) {
                            stockProfitLossResult.setQuantity(2 * stockProfitLossResult.getQuantity());
                            isAveragePriceHit = true;

                        }*/

						if (isEntered) {
							if (quote.getHigh().intValue() >= data.getTargetPrice1() || quote.getHigh().intValue() >= data.getTargetPrice2()) {
								stockProfitLossResult.setSellPrice(quote.getHigh().intValue());
								stockProfitLossResult.setSellTime(LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()));
								isTargetAchieved = true;
								sb.append(" Target Achieved at: ").append(quote.getDate().getTime()).append(" with price: ").append(quote.getHigh().intValue()).append("\n");
							} else if (quote.getLow().intValue() <= (isAverageAsStopLoss ? data.getAveragePrice() : data.getStopLoss())) {
								stockProfitLossResult.setSellPrice(quote.getLow().intValue());
								stockProfitLossResult.setSellTime(LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()));
								isStopLossHit = true;
								sb.append(" Stop Loss Hit at: ").append(quote.getDate().getTime()).append(" with price: ").append(quote.getLow().intValue()).append("\n");
							}
						}
						if (isTargetAchieved || isStopLossHit) {

							break; // Exit loop if target or stop loss is hit
						}

					}
				} else {

					for (int i = 0; i < completeResult.size() - 1; i++) {

						HistoricalQuote quote = completeResult.get(i);

						if (quote.getHigh() == null || quote.getLow() == null || quote.getHigh().intValue() == 0 || quote.getLow().intValue() == 0) {
							continue; // Skip quotes with null values
						}
						if (!isEntered && (quote.getHigh().intValue() > data.getEntryPrice1() || quote.getHigh().intValue() > data.getEntryPrice2())) {
							stockProfitLossResult = new StockProfitLossResult();
							stockProfitLossResult.setStock(data.getStock());
							stockProfitLossResult.setDate(data.getDate());
							stockProfitLossResult.setType("Negative");
							stockProfitLossResult.setSellPrice(quote.getHigh().intValue());
							stockProfitLossResult.setSellTime(LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()));
							int quantity = (int) (100000 / stockProfitLossResult.getSellPrice());
							stockProfitLossResult.setQuantity(quantity);
							sb.append("Negative Stock ").append(data.getStock()).append(" Entered at: ").append(quote.getDate().getTime()).append(" with price: ").append(quote.getLow());
							isEntered = true;
						}

                        /*if (!isAveragePriceHit && quote.getHigh().intValue() > data.getAveragePrice()) {
                            stockProfitLossResult.setQuantity(2 * stockProfitLossResult.getQuantity());
                            isAveragePriceHit = true;
                        }*/

						if (isEntered) {
							if (quote.getLow().intValue() <= data.getTargetPrice1() || quote.getLow().intValue() <= data.getTargetPrice2()) {
								stockProfitLossResult.setBuyPrice(quote.getLow().intValue());
								stockProfitLossResult.setBuyTime(LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()));
								isTargetAchieved = true;
								sb.append(" Target Achieved at: ").append(quote.getDate().getTime()).append(" with price: ").append(quote.getHigh()).append("\n");
							} else if (quote.getHigh().intValue() >= (isAverageAsStopLoss ? data.getAveragePrice() : data.getStopLoss())) {
								stockProfitLossResult.setBuyPrice(quote.getHigh().intValue());
								stockProfitLossResult.setBuyTime(LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()));
								isStopLossHit = true;
								sb.append(" Stop Loss Hit at: ").append(quote.getDate().getTime()).append(" with price: ").append(quote.getLow()).append("\n");
							}
						}

						if (isTargetAchieved || isStopLossHit) {

							break; // Exit loop if target or stop loss is hit
						}

					}

				}
				if (sb.toString().length() > 10) {
					stockNames.add(sb.toString());
				}
				if (stockProfitLossResult != null && stockProfitLossResult.getSellPrice() > 0 && stockProfitLossResult.getBuyPrice() > 0) {
					if (stockProfitLossResult.getType().equals("Positive")) {
						stockProfitLossResult.setTotal(stockProfitLossResult.getSellPrice() * stockProfitLossResult.getQuantity() - stockProfitLossResult.getBuyPrice() * stockProfitLossResult.getQuantity());
					} else {
						stockProfitLossResult.setTotal(stockProfitLossResult.getBuyPrice() * stockProfitLossResult.getQuantity() - stockProfitLossResult.getSellPrice() * stockProfitLossResult.getQuantity());
					}
					// stockResultManager.saveStockProfitLoss(stockProfitLossResult);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if (!stockNames.isEmpty()) {
			//mailService.sendMail(stockNames, properties);
		} else {
			log.info("No records found for verifyStockData");
		}
		return stockNames;
	}


	@GetMapping("/deleteStockByDate")
	public void deleteStockByDate(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate) {
		if (!stockDate.isEmpty()) {
			stockDataManager.deleteStocksByDate(stockDate);
		}
	}

	@GetMapping("/deleteStockByDateRange")
	public void deleteStockByDateRange(@RequestParam(name = "stockDate", required = false, defaultValue = "") String fromstockDate,
	                                   @RequestParam(name = "toStockDate", required = false, defaultValue = "") String toStockDate) {
		if (!fromstockDate.isEmpty() && !toStockDate.isEmpty()) {
			stockDataManager.deleteStocksByDateRange(fromstockDate, toStockDate);
		}
	}


	@GetMapping("/totalProfitLoss")
	public List<StockProfitLossResult> totalProfitLoss(@RequestParam(name = "stockDate", required = false, defaultValue = "") String fromstockDate) {
		if (!fromstockDate.isEmpty()) {
			return stockResultManager.getResultsByDate(fromstockDate);
		}
		return Collections.EMPTY_LIST;
	}


	@GetMapping("/deleteResultsByDate")
	public void deleteResultsByDate(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate) {
		if (!stockDate.isEmpty()) {
			stockResultManager.deleteResultsByDate(stockDate);
		}
	}

}