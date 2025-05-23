package com.stocks.controller;

import com.stocks.dto.Properties;
import com.stocks.dto.StockResponse;
import com.stocks.mail.Mail;
import com.stocks.service.DayHighLowService;
import com.stocks.service.MarketDataService;
import com.stocks.service.OptionChainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stocks.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

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
            if(!list1.isEmpty()) {
                data = mailService.beautifyResults(list1, properties);
                mailService.sendMail(data, properties);
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
                                @RequestParam(name = "exitMins", required = false, defaultValue = "0") int exitMins) {
        Properties properties = new Properties();
        properties.setStockDate(stockDate);
        properties.setExitMins(exitMins);
        properties.setFetchAll(fetchAll);
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

    @GetMapping("/oiChange")
    public List<String> sector( @RequestParam (name = "stockDate", required = false, defaultValue = "") String stockDate,
                          @RequestParam (name = "interval", required = false, defaultValue = "0") Integer interval){
        Properties properties = new Properties();
        properties.setStockDate(stockDate);
        properties.setInterval(interval);
        List<String> oiChange = futureEodAnalyzerService.getOIChange(properties);
        return  oiChange;
    }

    @GetMapping("/oiChangeForSector")
    public List<String> oiChangeForSector( @RequestParam (name = "stockDate", required = false, defaultValue = "") String stockDate,
                                @RequestParam (name = "interval", required = false, defaultValue = "0") Integer interval){
        Properties properties = new Properties();
        properties.setStockDate(stockDate);
        properties.setInterval(interval);
        List<String> oiChange = futureEodAnalyzerService.getOIChangeForSector(properties);
        return  oiChange;
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
            if(!list1.isEmpty()) {
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

        return  "success";
    }

}