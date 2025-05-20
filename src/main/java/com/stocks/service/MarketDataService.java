package com.stocks.service;

import com.stocks.dto.*;
import com.stocks.dto.FutureEodAnalyzer;
import com.stocks.dto.Properties;
import com.stocks.utils.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.Month.url")
    private String apiMonthUrl;

    @Value("${api.auth.token}")
    private String authToken;

    @Value("${endTime}")
    private String endTime;

    @Value("${eodValue}")
    private int eodValue;

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);


    public List<StockResponse> callApi(Properties properties) {

        // Path to the file containing the stock list
        String filePath = "src/main/resources/stocksList.txt";

        // Read all lines from the file into a List
        List<String> stockList;
        if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
            stockList = Arrays.asList(properties.getStockName().split(","));
        } else {
            try (java.util.stream.Stream<String> lines = Files.lines(Paths.get(filePath))) {
                stockList = lines.collect(Collectors.toList());
            } catch (IOException e) {
                log.error("Error reading file: " + e.getMessage());
                return null;
            }
        }

        // Process stocks in parallel
        List<StockResponse> emailList =
                ("test".equalsIgnoreCase(properties.getEnv()) ? stockList.stream() : stockList.parallelStream())
                        .map(stock -> {
                            return processStock(stock, properties);
                        }).filter(Objects::nonNull).collect(Collectors.toList());

        if ("test".equalsIgnoreCase(properties.getEnv())) {
            List<StockResponse> list = emailList.stream()
                    .filter(stock -> "".equals(stock.getTrend()))
                    .collect(Collectors.toList());
            testingResultsService.testingResults(list, properties);
        }

        return emailList;
    }

    private StockResponse processStock(String stock, Properties properties) {
        try {
            List<Candle> candles = processCandleSticks.getCandles(properties, stock);
            StockResponse res = processCandleSticks.getStockResponse(stock,properties, candles);
            if (res != null && res.getCurCandle().getHigh()>100) {
//                if (Math.abs(res.getChgeInPer()) > 1) {
//                    return null;
//                }
                StockData s1 = stockDataManager.getRecord(stock, FormatUtil.getCurDate(properties), FormatUtil.formatTimeHHmm(res.getCurCandle().getStartTime()));
                if (s1 == null) {
                    if (!res.getStockType().isEmpty()) {
                        List<String> details = calculateOptionChain.processStock(res.getStock(), res.getStockType(), FormatUtil.formatTimeHHmmss(res.getCurCandle().getStartTime()), properties);
                        if(!details.isEmpty()) {
                            res.setOptionChain(details.get(0));
                        }
                        stockDataManager.saveStockData(stock, FormatUtil.getCurDate(properties), FormatUtil.formatTimeHHmm(res.getCurCandle().getStartTime()), "", res.getStockType(), res.getLimit());
                        processEodResponse(res);
//                    optionChainService.processOptionChain(res, properties);
//                        if(res.getStockType().equals("N")) {
//                            if("+ve".equals(res.getOptionChain())){
//                                return res;
//                            }
//                        } else if (res.getStockType().equals("P")) {
//                            if("-ve".equals(res.getOptionChain())){
//                                return res;
//                            }
//                        }
                        return res;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing stock: {}, {}", stock, e.getMessage(), e);
        }
        return null;
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
            if (res.getCurLow() < dayM1.getInDayLow()) {
                res.setYestDayBreak("Y");
            }
            res.setPriority(determinePriority(oi1, "SBU", "LU"));
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