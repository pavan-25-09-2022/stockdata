package com.stocks.service;

import com.stocks.dto.FutureAnalysis;
import com.stocks.dto.Properties;
import com.stocks.dto.StockResponse;
import com.stocks.mail.Mail;
import com.stocks.utils.FormatUtil;
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
    private FutureEodAnalyzerService futureEodAnalyzerService;

//    @Scheduled(cron = "10 */5 9,10 * * *") // Runs from 9:15 to 9:35, 11:15 to 11:35, and 14:15 to 14:35
    public void callApi() {
        log.info("Scheduler started API stocks");
        logTime();
        Properties properties = new Properties();
        properties.setInterval(5);
        properties.setExpiryDate("250529");
        List<StockResponse> list = apiService.callApi(properties);
        if (list == null || list.isEmpty()) {
            log.info("No records found");
            return;
        }
        String data = mailService.beautifyResults(list, properties);
        mailService.sendMail(data, properties);
        System.gc();
        log.info("Scheduler finished " + list.size());
    }

    @Scheduled(cron = "50 30/15 9-16 * * ?")
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
        if(list.isEmpty()){
            log.info("No records found");
            return;
        }
        String data = mailService.beautifyOptChnResults(list);
            mailService.sendOptMail(data);
        log.info("Scheduler finished");
    }

//        @Scheduled(cron = "15 */5 9-14 * * *") // Starts at 9:18:02 and runs every 3 minutes until 15:00
    public void eodTrendAnalyser(){
            log.info("Scheduler started");
            Properties properties = new Properties();
            String selectedDate =  LocalDate.now().toString();

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


//    @Scheduled(cron = "50 0/5 9-15 * * ?")
    public void sector() {
        log.info("Scheduler started");
        logTime();
        Properties properties = new Properties();
        properties.setInterval(5);
         properties.setStockDate("2025-05-19");
        futureEodAnalyzerService.getTrendLinesForNiftyAndBankNifty(properties);;
        System.gc();
    }

}
