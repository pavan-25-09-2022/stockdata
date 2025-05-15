package com.stocks.controller;

import com.stocks.dto.Properties;
import com.stocks.dto.StockResponse;
import com.stocks.mail.Mail;
import com.stocks.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApiController {

    @Autowired
    private MarketDataService apiService;

    @Autowired
    private DayHighLowService dayHighLowService;

    @Autowired
    private FutureAnalysisService futureAnalysisService;

    @Autowired
    private FutureEodAnalyzerService futureEodAnalyzerService;

    @Autowired
    OpenHighLowService openHighLowService;

    @Autowired
    private Mail mailService;

    @GetMapping("/call-api")
    public String callApi( @RequestParam (name = "stockDate", required = false, defaultValue = "") String stockDate,
                           @RequestParam (name = "interval", required = false, defaultValue = "0") Integer interval,
                          @RequestParam (name = "fetchAll", required = false) boolean fetchAll,
                          @RequestParam (name = "exitMins", required = false, defaultValue = "0") int exitMins,
                           @RequestParam (name = "amtInvested", required = false, defaultValue = "0") int amtInvested,
                           @RequestParam (name = "stockName", required = false, defaultValue = "") String stockName ) {
        Properties properties = new Properties();
        properties.setStockDate(stockDate);
        properties.setExitMins(exitMins);
        properties.setFetchAll(fetchAll);
        properties.setInterval(interval);
        properties.setAmtInvested(amtInvested);
        properties.setStockName(stockName);
        List<StockResponse> list = apiService.callApi(properties);
        if (list == null || list.isEmpty()) {
            return "No data found";
        }
        String data = mailService.beautifyResults(list);
        mailService.sendMail(data);
        System.gc();
        return data;
    }

    @GetMapping("/apiDayHighLow")
    public String apiDayHighLow( @RequestParam (name = "stockDate", required = false, defaultValue = "") String stockDate,
                                 @RequestParam (name = "fetchAll", required = false) boolean fetchAll,
                                 @RequestParam (name = "exitMins", required = false, defaultValue = "0") int exitMins) {
        Properties properties = new Properties();
        properties.setStockDate(stockDate);
        properties.setExitMins(exitMins);
        properties.setFetchAll(fetchAll);
        List<StockResponse> list = dayHighLowService.dayHighLow(properties);
        if (list == null || list.isEmpty()) {
            return "No data found";
        }
        String data = mailService.beautifyResults(list);
        mailService.sendMail(data);
        System.gc();
        return data;
    }

    @GetMapping("/sector")
    public String sector( @RequestParam (name = "stockDate", required = false, defaultValue = "") String stockDate,
                         @RequestParam (name = "interval", required = false, defaultValue = "0") Integer interval,
                                 @RequestParam (name = "fetchAll", required = false) boolean fetchAll,
                                 @RequestParam (name = "exitMins", required = false, defaultValue = "5") int exitMins) {
        Properties properties = new Properties();
        properties.setStockDate(stockDate);
        properties.setExitMins(exitMins);
        properties.setFetchAll(fetchAll);
        properties.setInterval(interval);
        futureEodAnalyzerService.getTrendLinesForNiftyAndBankNifty(properties);
        return  "success";
    }

    @GetMapping("/eodAnalyzer")
    public String eodAnalyzer(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
                              @RequestParam (name = "interval", required = false, defaultValue = "0") Integer interval) {
        Properties properties = new Properties();
        properties.setStockDate(stockDate);
        properties.setInterval(interval);
        if(interval < 5){
            properties.setInterval(5);
        }
        return futureEodAnalyzerService.processEodResponse(properties);
    }

    @GetMapping("/notDayHighLow")
    public List<String> eodAnalyzer(@RequestParam (name = "interval", required = false, defaultValue = "0") Integer interval) {
        Properties properties = new Properties();
        return openHighLowService.dayHighLow(properties);
    }

}