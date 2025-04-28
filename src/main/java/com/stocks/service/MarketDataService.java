package com.stocks.service;

import com.stocks.dto.*;
import com.stocks.utils.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

    private static final int MINS = 3;
    private static final LocalTime START_TIME = LocalTime.of(9, 16, 0); // Configurable start time

    @Autowired
    private IOPulseService ioPulseService;
    @Autowired
    private TestingResultsService testingResultsService;
    @Autowired
    private StockDataManager stockDataManager;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.Month.url")
    private String apiMonthUrl;

    @Value("${api.auth.token}")
    private String authToken;

    @Value("${date}")
    private String date;

    @Value("${endTime}")
    private String endTime;

    @Value("${eodValue}")
    private int eodValue;

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);


    public List<StockResponse> callApi() {

        String selectedDate = (date != null && !date.isEmpty()) ? date : LocalDate.now().toString();

        // Path to the file containing the stock list
        String filePath = "src/main/resources/stocksList.txt";

        // Read all lines from the file into a List
        List<String> stockList;
        try (java.util.stream.Stream<String> lines = Files.lines(Paths.get(filePath))) {
            stockList = lines.collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error reading file: " + e.getMessage());
            return null;
        }

        // Process stocks in parallel
        List<StockResponse> emailList = stockList.parallelStream().map(stock -> {
            try {
                // Make POST request
                ResponseEntity<ApiResponse> response = ioPulseService.sendRequest(stock, selectedDate);

                // Process response
                ApiResponse apiResponse = response.getBody();
                if (apiResponse == null || apiResponse.getData().size() <= 6) {
                    log.info("Data size is less than or equal to 6 for stock: " + stock);
                    return null;
                }
//                if (apiResponse.getData().get(0).getHigh() < 150 || apiResponse.getData().get(0).getHigh() > 30000) {
//                    return null;
//                }
                if (apiResponse.getData().get(0).getTradedVolume() < 10000) {
                    return null;
                }

                StockResponse res = processApiResponse(apiResponse, stock);
                if (res != null) {
                    processEodResponse(res);
                }
                return res;
            } catch (Exception e) {
                log.error("Error processing stock: " + stock + ", " + e.getMessage());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

//        testingResultsService.testingResults(emailList, selectedDate);

        return emailList;
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
        StockEODDataResponse dayM1 = eod.getData().get(eodValue);
        StockEODDataResponse dayM2 = eod.getData().get(eodValue + 1);
        StockEODDataResponse dayM3 = eod.getData().get(eodValue + 2);

        // Calculate changes and interpretations
        String oi1 = calculateOiInterpretation(dayM1, dayM2);
        String oi2 = calculateOiInterpretation(dayM2, dayM3);
        res.setEodData(oi1 + ", " + oi2);

        // Determine priority based on stock type and interpretations
        if ("N".equals(res.getStockType()) && "SBU".equals(res.getOiInterpretation())) {
            res.setPriority(determinePriority(oi1, "SBU", "LU"));
        } else if ("P".equals(res.getStockType()) && "LBU".equals(res.getOiInterpretation())) {
            res.setPriority(determinePriority(oi1, "LBU", "SC"));
        }
    }

    private String calculateOiInterpretation(StockEODDataResponse current, StockEODDataResponse previous) {
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


    public List<List<ApiResponse.Data>> chunkByMinutes(List<ApiResponse.Data> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            log.warn("Data list is null or empty, returning an empty list.");
            return Collections.emptyList();
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Group data by intervals
        Map<LocalTime, List<ApiResponse.Data>> groupedData = dataList.stream()
                .collect(Collectors.groupingBy(
                        data -> {
                            LocalTime time = LocalTime.parse(data.getTime(), timeFormatter);
                            int minutesSinceStart = (int) java.time.Duration.between(START_TIME, time).toMinutes();
                            int minuteBucket = (minutesSinceStart / MINS) * MINS;
                            return START_TIME.plusMinutes(minuteBucket).withSecond(0);
                        },
                        LinkedHashMap::new, // Maintain order
                        Collectors.toList()
                ));
        return new ArrayList<>(groupedData.values());
    }

    private StockResponse processApiResponse(ApiResponse apiResponse, String stock) {
        List<ApiResponse.Data> list = apiResponse.getData();
        ApiResponse.Data previousData;
//        List<Long> volumes = new ArrayList<>();
        long previousVolume = 0;
        double firstCandleHigh = 0.0;
        double firstCandleLow = 0.0;
        List<List<ApiResponse.Data>> chunks = chunkByMinutes(list);
        if (chunks.size() < 3) {
            log.info("Data size is less than 3 for stock: " + stock);
            return null;
        }

        List<ApiResponse.Data> newList = chunks.get(1);
        for (ApiResponse.Data data : newList) {
            previousVolume += data.getTradedVolume();
            if (firstCandleHigh == 0.0) {
                firstCandleHigh = data.getHigh();
            }
            if (firstCandleLow == 0.0) {
                firstCandleLow = data.getLow();
            }
            firstCandleHigh = Math.max(data.getHigh(), firstCandleHigh);
            firstCandleLow = Math.min(data.getLow(), firstCandleLow);
        }
        String recentTime = stockDataManager.getRecentTime(stock, FormatUtil.getCurDate());
        LocalTime recentTimeStamp = null;
        if (recentTime != null) {
            recentTimeStamp = LocalTime.parse(recentTime);
        }

        long highVolume = previousVolume;
        previousData = newList.get(newList.size() - 1);
        for (int i = 2; i < chunks.size() - 1; i++) {
            List<ApiResponse.Data> chunk = chunks.get(i);
            long totalVolume = 0;
            ApiResponse.Data recentData = null;
            ApiResponse.Data firstCandle = null;

            double curOpen = 0.0;
            double curClose = 0.0;
            for (ApiResponse.Data data : chunk) {
                totalVolume += data.getTradedVolume();
                recentData = data;
                if (firstCandle == null) {
                    firstCandle = data;
                }
                if (curOpen == 0.0) {
                    curOpen = data.getOpen();
                }
                if (curClose == 0.0) {
                    curClose = data.getClose();
                }
                curOpen = Math.min(data.getOpen(), curOpen);
                curClose = Math.min(data.getClose(), curClose);
            }

//            double curOpen = chunk.get(0).getOpen();
//            double curClose = chunk.get(chunk.size() - 1).getClose();
            if (recentData == null) {
                continue;
            }
            LocalTime localTime = LocalTime.parse(recentData.getTime());
            LocalTime endLocalTime = LocalTime.parse(endTime);
            if (localTime.isAfter(endLocalTime)) {
                return null;
            }
            double ltpChange = recentData.getClose() - previousData.getClose();
            ltpChange = Double.parseDouble(String.format("%.2f", ltpChange));
            long oiChange = Long.parseLong(recentData.getOpenInterest()) - Long.parseLong(previousData.getOpenInterest());
            String oiInterpretation = (oiChange > 0)
                    ? (ltpChange > 0 ? "LBU" : "SBU")
                    : (ltpChange > 0 ? "SC" : "LU");

            boolean isHigher = false;
            if (highVolume < totalVolume) {
                isHigher = true;
                highVolume = totalVolume;
            }
//            if (!oiInterpretation.contains("BU")) {
//                return null;
//            }
            previousData = chunk.get(chunk.size() - 1);
            if (recentTimeStamp != null) {
                if (localTime.isBefore(recentTimeStamp)) {
                    continue;
                }
            }
            if (recentTimeStamp == null || localTime.isAfter(recentTimeStamp)) {
                if (firstCandleLow > recentData.getClose() && (oiInterpretation.equals("SBU")) && isHigher) {
                    stockDataManager.saveStockData(stock, FormatUtil.getCurDate(), recentData.getTime(), recentData.getOpenInterest());
                    return new StockResponse(stock, "N", firstCandle.getTime(), recentData.getTime(), oiInterpretation, firstCandleHigh, curClose, totalVolume);
                }
                if (firstCandleHigh < recentData.getOpen() && (oiInterpretation.equals("LBU")) && isHigher) {
                    stockDataManager.saveStockData(stock, FormatUtil.getCurDate(), recentData.getTime(), recentData.getOpenInterest());
                    return new StockResponse(stock, "P", firstCandle.getTime(), recentData.getTime(), oiInterpretation, firstCandleLow, curClose, totalVolume);
                }
            }
        }
        return null;
    }
}