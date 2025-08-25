package com.stocks.service;

import com.stocks.controller.ApiController;
import com.stocks.dto.Properties;
import com.stocks.dto.StockResponse;
import com.stocks.dto.TradeSetupTO;
import com.stocks.mail.Mail;
import com.stocks.mail.MarketMoversMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class Scheduler {

	private static final Logger log = LoggerFactory.getLogger(Scheduler.class);
	@Autowired
	private MarketDataService apiService;
	@Autowired
	private DayHighLowService dayHighLowService;
	@Autowired
	private Mail mailService;
	@Autowired
	private OptionChainService optionChainService;
	@Autowired
	private FutureEodAnalyzerService futureAnalysisService;
	@Autowired
	private TodayFirstCandleTrendLine todayFirstCandleTrendLine;
	@Autowired
	private MarketMovers marketMovers;
	@Autowired
	private MarketMoversMailService marketMoversMailService;
	@Autowired
	private ApiController apiController;
	@Autowired
	private FutureEodAnalyzerService futureEodAnalyzerService;
	@Autowired
	private DayHighLowBreakService dayHighLowBreakService;

	@Scheduled(cron = "10 */5 9,10 * * *") // Runs from 9:15 to 9:35, 11:15 to 11:35, and 14:15 to 14:35
	public void callApi() {
		log.info("Scheduler started API stocks");
		logTime();
		Properties properties = new Properties();
		properties.setInterval(5);
		properties.setExpiryDate("250529");
		properties.setStartTime("09:15");
		List<TradeSetupTO> list = apiService.callApi(properties);
		if (list == null || list.isEmpty()) {
			log.info("No records found");
			return;
		}
		String data = marketMoversMailService.beautifyResults(list);
		marketMoversMailService.sendMail(data, properties, "API Stocks Report");
		System.gc();
		log.info("Scheduler finished " + list.size());
	}

	// @Scheduled(cron = "50 30/15 9-16 * * ?")
	public void dayHighLow() {
		log.info("Scheduler started");
		Properties properties = new Properties();
		properties.setInterval(15);
		properties.setCheckRecentCandle(true);
		dayHighLowService.dayHighLow(properties);
		log.info("Scheduler finished");
	}

	//    @Scheduled(cron = "15 */5 9-14 * * *") // Starts at 9:18:02 and runs every 3 minutes until 15:00
	public void optionChain() {
		log.info("Scheduler started");
		Properties properties = new Properties();
		properties.setInterval(5);
		properties.setExpiryDate("250529");
		List<StockResponse> list = optionChainService.getOptionChain(properties);
		if (list.isEmpty()) {
			log.info("No records found");
			return;
		}
		String data = mailService.beautifyOptChnResults(list);
		mailService.sendOptMail(data);
		log.info("Scheduler finished");
	}

	//        @Scheduled(cron = "15 */5 9-14 * * *") // Starts at 9:18:02 and runs every 3 minutes until 15:00
	public void eodTrendAnalyser() {
		log.info("Scheduler started");
		Properties properties = new Properties();
		String selectedDate = LocalDate.now().toString();

		properties.setStockDate(selectedDate);
		properties.setInterval(5);
		futureAnalysisService.getTrendLinesForNiftyAndBankNifty(properties);
	}

	//    @Scheduled(cron = "10 */5 9,12 * * *") // Runs from 9:15 to 9:35, 11:15 to 11:35, and 14:15 to 14:35
	public void trendLines() {
		log.info("Scheduler started Trend stocks");
		logTime();
		Properties properties = new Properties();
		properties.setInterval(5);
		properties.setExpiryDate("250529");
		List<StockResponse> list = todayFirstCandleTrendLine.getTrendLines(properties);
		if (list == null || list.isEmpty()) {
			log.info("No records found");
			return;
		}
		String data = mailService.beautifyResults(list, properties);
		mailService.sendTrendsMail(data, properties);
		System.gc();
		log.info("Scheduler finished trends " + list.size());
	}

	public static void logTime() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
		String currentTime = LocalTime.now().format(formatter);
		log.info("Current Time: " + currentTime);
	}


	//@Scheduled(cron = "50 0/5 9-15 * * ?")
	public void sector() {
		log.info("Scheduler started for Trend Lines for Nifty and Bank Nifty 5 minutes");
		logTime();
		Properties properties = new Properties();
		properties.setInterval(5);
		properties.setStockDate("2025-08-11");
		futureAnalysisService.getTrendLinesForNiftyAndBankNifty(properties);
		System.gc();
	}

	//	@Scheduled(cron = "50 0/5 9-15 ? * MON-FRI")
	public void marketMavesAndOptionChain() {
		log.info("Scheduler started Market Movers and Option Chain");
		logTime();
		Properties properties = new Properties();
		properties.setInterval(5);
		properties.setStockDate(LocalDate.now().toString());
		futureEodAnalyzerService.createOptionChainImages(properties);
		System.gc();
		log.info("Scheduler finished Market Movers and Option Chain ");
	}

	@Scheduled(cron = "20 0/5 9-15 ? * MON-FRI")
	public void marketMoverGainers() {
		log.info("Scheduler started Market Movers Gainers and Option Chain");
		Properties properties = buildProperties();
		List<TradeSetupTO> trades = marketMovers.marketMoverDetails(properties, "G");
		if (!trades.isEmpty()) {
			String data = marketMoversMailService.beautifyResults(trades);
			marketMoversMailService.sendMail(data, properties, "Market Movers Report");
		}
		log.info("Scheduler finished Market Movers Gainers and Option Chain ");
		System.gc();
	}

	//	@Scheduled(cron = "30 0/5 9-15 * * ?")
	public void marketMoverLosers() {
		log.info("Scheduler started Market Movers Losers and Option Chain");
		Properties properties = buildProperties();
		marketMovers.marketMoverDetails(properties, "L");
		System.gc();
		log.info("Scheduler finished Market Movers Losers and Option Chain ");
	}

	@Scheduled(cron = "20 0/15 9-15 ? * MON-FRI")
	public void optionData() {
		log.info("Scheduler started Option Chain");
		Properties properties = buildProperties();
		List<TradeSetupTO> trades = marketMovers.optionChainData(properties);
		if (!trades.isEmpty()) {
			String data = marketMoversMailService.beautifyResults(trades);
			marketMoversMailService.sendMail(data, properties, "Market Movers Report");
		}
		log.info("Scheduler finished Option Chain ");
		System.gc();
	}

	//	@Scheduled(cron = "50 0/5 9-15 * * ?")
	public void verifyStockData() {
		log.info("Scheduler started verifyStockData");
		logTime();
		apiController.verifyStockData(LocalDate.now().toString(), 5, false);
		System.gc();
		log.info("Scheduler finished Verify Stock Data ");
	}

	private Properties buildProperties() {
		Properties properties = new Properties();
		properties.setStockDate(LocalDate.now().toString());
		properties.setInterval(5);
		return properties;
	}

	@Scheduled(cron = "40 0/5 9-15 ? * MON-FRI")
	private void getStocksBasedOnHighChangeInOpenInterest() {
		log.info("Scheduler started getStocksBasedOnHighChangeInOpenInterest");
		Properties properties = buildProperties();
		futureEodAnalyzerService.getStocksBasedOnHighChangeInOpenInterest(properties);
		log.info("Scheduler finished getStocksBasedOnHighChangeInOpenInterest ");

	}

	//@Scheduled(cron = "30 20-59/5,0-59/5 9,10,11,12,13,14,15 * * MON-FRI")
	private void testStockData() {
		log.info("Scheduler started testStockData");
		Properties properties = new Properties();
		properties.setInterval(5);
		properties.setStartDate(LocalDate.now().toString());
		properties.setEndDate(LocalDate.now().toString());
		List<String> strings = futureEodAnalyzerService.testStocks(properties);
		if (strings != null && !strings.isEmpty()) {
			marketMoversMailService.sendMail(strings.toString(), properties, "Test Stock Data Report");
		}
		System.gc();
		log.info("Scheduler finished testStockData ");
	}

	@Scheduled(cron = "0 20-59,0-59 9,10,11,12,13,14 * * MON-FRI")
	private void dayHighLowBreak() {
		log.info("Scheduler started Day High Low");
		dayHighLowBreakService.testDayHighLow();
		log.info("Scheduler finished Day High Low");
		System.gc();
	}
}
