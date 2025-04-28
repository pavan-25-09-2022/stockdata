package com.stocks.service;

import com.stocks.dto.StockResponse;
import com.stocks.mail.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Scheduled(cron = "2 18/3 9-14 * * *") // Starts at 9:18:02 and runs every 3 minutes until 15:00
    public void callApi() {
        log.info("Scheduler started");
        logTime();
        List<StockResponse> list = apiService.callApi();
        if (list == null || list.isEmpty()) {
           log.info("No records found");
            return;
        }
        String data = mailService.beautifyResults(list);
        mailService.sendMail(data);
        System.gc();
        log.info("Scheduler finished " + list.size());
    }

    @Scheduled(cron = "2 18/3 9-14 * * *") // Starts at 9:18:02 and runs every 3 minutes until 15:00
    public void dayHighLow() {
//        log.info("Scheduler started");
//        dayHighLowService.dayHighLow();
//        log.info("Scheduler finished");
    }

    public static void logTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String currentTime = LocalTime.now().format(formatter);
        log.info("Current Time: " + currentTime);
    }
}
