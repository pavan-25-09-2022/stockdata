package com.stocks.service;

import com.stocks.dto.*;
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
    @Autowired
    private IOPulseService ioPulseService;
    @Autowired
    private TestingResultsService testingResultsService;

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
        StockEODResponse eod = ioPulseService.getMonthlyData(res.getStock());

        if (eod == null || eod.getData().isEmpty()) {
            log.info("EOD data is empty for stock: " + res.getStock());
            return;
        }
        int i = 1;
        StockEODDataResponse dayM1 = eod.getData().get(i);
        StockEODDataResponse dayM2 = eod.getData().get(i + 1);
        StockEODDataResponse dayM3 = eod.getData().get(i + 2);

        double ltpChange1 = dayM1.getInClose() - dayM2.getInClose();
        ltpChange1 = Double.parseDouble(String.format("%.2f", ltpChange1));
        long oiChange1 = Long.parseLong(dayM1.getInOi()) - Long.parseLong(dayM2.getInOi());
        String oi1 = (oiChange1 > 0)
                ? (ltpChange1 > 0 ? "LBU" : "SBU")
                : (ltpChange1 > 0 ? "SC" : "LU");

        double ltpChange2 = dayM2.getInClose() - dayM3.getInClose();
        ltpChange2 = Double.parseDouble(String.format("%.2f", ltpChange2));
        long oiChange2 = Long.parseLong(dayM2.getInOi()) - Long.parseLong(dayM3.getInOi());
        String oi2 = (oiChange2 > 0)
                ? (ltpChange2 > 0 ? "LBU" : "SBU")
                : (ltpChange2 > 0 ? "SC" : "LU");
        String eodIo = oi1 + ", " + oi2;
        res.setEodData(eodIo);
        if (res.getStockType().equals("N")) {
            if (res.getOiInterpretation().equals("SBU")) {
                if (oi1.equals("SBU")) {
                    res.setPriority(1);
                } else if (oi1.equals("LU")) {
                    res.setPriority(2);
                } else {
                    res = null;
                }
            }
        } else {
            if (res.getOiInterpretation().equals("LBU")) {
                if (oi1.equals("LBU")) {
                    res.setPriority(1);
                } else if (oi1.equals("SC")) {
                    res.setPriority(2);
                } else {
                    res = null;
                }
            }
        }
    }

    public List<List<ApiResponse.Data>> chunkByMinutes(List<ApiResponse.Data> dataList) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Group data by 5-minute intervals
//        Map<LocalTime, List<ApiResponse.Data>> groupedData = dataList.stream()
//                .collect(Collectors.groupingBy(
//                        data -> {
//                            LocalTime time = LocalTime.parse(data.getTime(), timeFormatter);
//                            int minuteBucket = (time.getMinute() / MINS) * MINS;
//                            return time.withMinute(minuteBucket).withSecond(0);
//                        },
//                        LinkedHashMap::new, // Use LinkedHashMap to maintain order
//                        Collectors.toList()
//                ));

        Map<LocalTime, List<ApiResponse.Data>> groupedData = dataList.stream()
                .collect(Collectors.groupingBy(
                        data -> {
                            LocalTime startTime = LocalTime.of(9, 16, 0); // Starting time
                            LocalTime time = LocalTime.parse(data.getTime(), timeFormatter);
                            int minutesSinceStart = (int) java.time.Duration.between(startTime, time).toMinutes();
                            int minuteBucket = (minutesSinceStart / 3) * 3;
                            return startTime.plusMinutes(minuteBucket).withSecond(0);
                        },
                        LinkedHashMap::new, // Use LinkedHashMap to maintain order
                        Collectors.toList()
                ));


//        List<List<ApiResponse.Data>> chunks = new ArrayList<>();
//        List<ApiResponse.Data> currentChunk = new ArrayList<>();
//        String currentOi = null;
//
//        for (ApiResponse.Data data : dataList) {
//            if (!data.getOpenInterest().equals(currentOi)) {
//                // If openInterest changes, start a new chunk
//                if (!currentChunk.isEmpty()) {
//                    chunks.add(new ArrayList<>(currentChunk));
//                }
//                currentChunk.clear();
//                currentOi = data.getOpenInterest();
//            }
//            currentChunk.add(data);
//        }
//
//        // Add the last chunk if not empty
//        if (!currentChunk.isEmpty()) {
//            chunks.add(currentChunk);
//        }
//
//        return chunks;

        // Convert the grouped data into a list of chunks
        return new ArrayList<>(groupedData.values());
    }

    private StockResponse processApiResponse(ApiResponse apiResponse, String stock) {
        List<ApiResponse.Data> list = apiResponse.getData();
        ApiResponse.Data previousData = list.get(0);
        List<Long> volumes = new ArrayList<>();
        long previousVolume = 0;
        ApiResponse.Data firstCandle = list.get(0);
        volumes.add(firstCandle.getTradedVolume());
        double firstCandleHigh = 0.0;
        double firstCandleLow = 0.0;
        volumes.add(previousVolume);
        List<List<ApiResponse.Data>> chunks = chunkByMinutes(list);
        if (chunks.size() < 3) {
            log.info("Data size is less than 3 for stock: " + stock);
            return null;
        }

        List<ApiResponse.Data> newList = chunks.get(1);
//        newList.add(chunks.get(2).get(0));
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
        previousData = newList.get(newList.size() - 1);
        log.info("first candle high " + firstCandleHigh + " first candle low " + firstCandleLow);
        for (int i = 2; i < chunks.size() - 1; i++) {
            List<ApiResponse.Data> chunk = chunks.get(i);
//            chunk.add(chunks.get(i+1).get(0));
            long totalVolume = 0;
            ApiResponse.Data recentData = null;
            double curOpen = 0.0;
            double curClose = 0.0;
            for (ApiResponse.Data data : chunk) {
                totalVolume += data.getTradedVolume();
                recentData = data;
                if (curOpen == 0.0) {
                    curOpen = data.getOpen();
                }
                if (curClose == 0.0) {
                    curClose = data.getClose();
                }
                curOpen = Math.min(data.getOpen(), curOpen);
                curClose = Math.min(data.getClose(), curClose);
                log.info("NS-S " + stock + " high " + data.getOpen() + " low " + data.getClose() + " @ " + recentData.getTime());
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

            long finalTotalVolume = totalVolume;
            boolean isHigher = volumes.stream().anyMatch(volume -> finalTotalVolume > volume);

            if (totalVolume < 10000) {
                return null;
            }
            log.info("NS-S " + stock + " oiChange " + oiChange + " high " + recentData.getHigh() + " low " + recentData.getLow() + " current " + curClose + "  " + oiInterpretation + (isHigher ? " volume high" : "") + " @ " + recentData.getTime());
            log.info("recent close " + recentData.getClose() + " previous close " + previousData.getClose() + " oiChange " + oiChange + " ltpChange " + ltpChange);
            log.info("curr candle high " + curOpen + " curr candle high  " + curClose);
//            boolean value = (oiInterpretation.contains("Long") && isHigher) || (oiInterpretation.contains("Short"));
            if (!oiInterpretation.contains("BU")) {
                return null;
            }
            if (firstCandleLow > recentData.getClose() && recentData.getClose() < recentData.getOpen() && oiInterpretation.equals("SBU")) {
                double stopLoss = firstCandleHigh;
//                String previousDate = LocalDate.parse(firstCandle.getDate()).minusDays(1).toString();
//                ResponseEntity<ApiResponse> response = sendRequest(stock, previousDate);
//                ApiResponse yesResponse = response.getBody();
//                assert yesResponse != null;
//                ApiResponse.Data lastRecord = yesResponse.getData().get(yesResponse.getData().size() - 1);
                return new StockResponse(stock, "N", recentData.getTime(), oiInterpretation, stopLoss, curClose, isHigher);
            }
            if (firstCandleHigh < recentData.getOpen() && recentData.getOpen() < recentData.getClose() && oiInterpretation.equals("LBU")) {
                double stopLoss = firstCandleLow;

//                String previousDate = LocalDate.parse(firstCandle.getDate()).minusDays(1).toString();
//                ResponseEntity<ApiResponse> response = sendRequest(stock, previousDate);
//                ApiResponse yesResponse = response.getBody();
//                assert yesResponse != null;
//                ApiResponse.Data lastRecord = yesResponse.getData().get(yesResponse.getData().size() - 1);
                return new StockResponse(stock, "P", recentData.getTime(), oiInterpretation, stopLoss, curClose, isHigher);
            }
            previousData = chunk.get(chunk.size() - 1);
        }
        return null;
    }
}