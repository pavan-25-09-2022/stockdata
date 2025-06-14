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
import java.time.LocalDate;
import java.time.LocalTime;
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
    @Autowired
    private CommonValidation commonValidation;


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
                ("test1".equalsIgnoreCase(properties.getEnv()) ? stockList.stream() : stockList.parallelStream())
                        .map(stock -> {
                            return processStock(stock, properties);
                        }).filter(Objects::nonNull).collect(Collectors.toList());

//        if ("test".equalsIgnoreCase(properties.getEnv())) {
//            List<StockResponse> list = emailList.stream()
//                    .filter(stock -> "".equals(stock.getTrend()))
//                    .collect(Collectors.toList());
//            testingResultsService.testingResults(list, properties);
//        }

        return emailList;
    }

    private StockResponse processStock(String stock, Properties properties) {
        try {
            List<Candle> candles = commonValidation.getCandles(properties, stock);
            Properties prop = new Properties();
            prop.setStockDate(FormatUtil.addDays(properties.getStockDate(), -1));
            prop.setStockName(properties.getStockName());
            List<Candle> ystCandles = commonValidation.getCandles(prop, stock);
            List<Candle> list = new ArrayList<>();
            StockResponse sr1 = null;
            Candle luCandle = null;
            Candle sbuCandle = null;
            Candle lbuCandle = null;
            List<Long> volumes = new ArrayList<>();
            for (Candle c : candles) {
                list.add(c);
                ystCandles.add(c);
                if (list.size() < 2) {
                    continue; // Skip if not enough candles
                }
                if (sr1 != null) {
                    lbuCandle = sr1.getValidCandle();
                    if (lbuCandle != null && "LBU".equals(c.getOiInt())) {
                        if (lbuCandle.getVolume() < c.getVolume()) {
                            lbuCandle = c;
                        }
                    }
//                    else if (lbuCandle != null && "LU".equals(c.getOiInt())) {
//                        if (luCandle == null) {
//                            luCandle = c;
//                        } else if (Math.abs(luCandle.getLtpChange()) > Math.abs(c.getLtpChange())) {
//                            luCandle = c;
//                        }
//                    }
                    else if (lbuCandle != null && "SBU".equals(c.getOiInt())) {
                        if (sbuCandle == null) {
                            sbuCandle = c;
                        } else if (Math.abs(c.getCandleStrength()) > Math.abs(lbuCandle.getCandleStrength())) {
                            sbuCandle = c;
                        }
                    }

                }
                volumes.add(c.getVolume());
                if (sr1 != null) {
                    Candle validCandle = sr1.getValidCandle();
                    if("test1".equalsIgnoreCase(properties.getEnv()) && sr1.getStock().contains("*")) {
                        commonValidation.checkExitSignal(sr1, c);
                    }
                    if (c.getVolume() > 1000 && validCandle != null && c.getEndTime().isBefore(LocalTime.of(10, 30)) && c.getOpen() <= validCandle.getLow() && c.getClose() > c.getOpen() && (c.getOiInt().equals("LBU") || c.getOiInt().equals("SC"))) {
                        if (lbuCandle != null) {
                            if(sbuCandle != null && Math.abs(sbuCandle.getLtpChange()) > Math.abs(lbuCandle.getLtpChange())){
//                            if (((sbuCandle != null && sbuCandle.getVolume() >= 0.9 * lbuCandle.getVolume() && Math.abs(sbuCandle.getLtpChange()) > Math.abs(lbuCandle.getLtpChange())) || (luCandle != null && luCandle.getVolume() >= 0.9 * lbuCandle.getVolume()) && luCandle.getLtpChange() > lbuCandle.getLtpChange())) {
                                continue;
                            }
                        }

                        sr1.setStock(stock + "*");
                        sr1.setCurCandle(c);
                    }
                } else {
                    StockResponse res = processCandleSticks.getStockResponse(stock, properties, list, ystCandles);

                    if (res != null && res.getCurCandle().getHigh() > 100) {
//                if (Math.abs(res.getChgeInPer()) > 1) {
//                    return null;
//                }
                        StockData s1 = stockDataManager.getRecord(stock, FormatUtil.getCurDate(properties), FormatUtil.formatTimeHHmm(res.getCurCandle().getStartTime()));
                        if (s1 == null) {
                            if (!res.getStockType().isEmpty() && "P".equals(res.getStockType())) {
//                            List<String> details = calculateOptionChain.processStock(res.getStock(), res.getStockType(), FormatUtil.formatTimeHHmmss(res.getCurCandle().getStartTime()), properties);
//                            if (!details.isEmpty()) {
//                                res.setOptionChain(details.get(0));
//                            }
                                if (!properties.isFetchAll()) {
                                    stockDataManager.saveStockData(stock, FormatUtil.getCurDate(properties), FormatUtil.formatTimeHHmm(res.getCurCandle().getStartTime()), "", res.getStockType(), res.getLimit());
                                }
//                       processEodResponse(res);
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
                                sr1 = res;
                            }
                        }
                    }
                }
            }
            if (sr1 != null && sr1.getStock().contains("*")) {
                    if ("test1".equalsIgnoreCase(properties.getEnv()) && sr1.getStockProfitResult() == null) {
                        log.info("{} first candle {}", sr1.getStock(), sr1.getFirstCandle());
                        log.info("{} current candle {}", sr1.getStock(), sr1.getCurCandle());
                        log.info("{} valid candle {}", sr1.getStock(), sr1.getValidCandle());
                        int i = 1;
                        boolean calculateForward = true;
                        int maxDays= 5;
                        String curDate = FormatUtil.addDays(LocalDate.now().toString(), 1);
                        Candle prevCandle = null;
                        while (calculateForward){
                            Thread.sleep(2000);
                            Properties prop1 = new Properties();
                            prop1.setStockDate(FormatUtil.addDays(properties.getStockDate(), i));
                            prop1.setStockName(properties.getStockName());
                            if(prop1.getStockDate().equals(curDate)){
                                calculateForward = false;
                            }
                            try {
                                List<Candle> plus1Candles = commonValidation.getCandles(prop1, stock);
                                log.info("Date {} candles {} curDate {}", prop1.getStockDate(), plus1Candles.size(), curDate);
                                for (Candle c : plus1Candles) {
                                    prevCandle = c;
                                    commonValidation.checkExitSignal(sr1, c);
                                    if (sr1.getStockProfitResult() != null) {
                                        String sell =                                sr1.getStockProfitResult().getSellTime();
                                        sr1.getStockProfitResult().setSellTime(sell + " " + prop1.getStockDate());
                                    }
                                }
                            }catch (Exception e){
                                calculateForward = false;
                            }
                            if(sr1.getStockProfitResult() != null){
                                calculateForward = false;
                            }
                            if(maxDays == i){
                                calculateForward = false;
                            }
                            i++;
                        }

                    }

                return sr1;
            } else {
                return sr1;
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

