package com.stocks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {

    private static final Logger log = LoggerFactory.getLogger(Scheduler.class);
    @Autowired
    private MarketDataService apiService;
    @Autowired
    private DayHighLowService dayHighLowService;

    @Scheduled(cron = "2 18/3 9-10 * * *") // Starts at 9:18:02 and runs every 3 minutes until 15:00
    public void callApi() {
        log.info("Scheduler started");
        apiService.callApi();
        log.info("Scheduler finished");
    }

    @Scheduled(cron = "2 18/3 9-10 * * *") // Starts at 9:18:02 and runs every 3 minutes until 15:00
    public void dayHighLow() {
        log.info("Scheduler started");
        dayHighLowService.dayHighLow();
        log.info("Scheduler finished");
    }
}
