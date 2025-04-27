package com.stocks.service;

import com.stocks.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Deprecated
public class ApiTestingServiceNew1 {

    private static final Logger log = LoggerFactory.getLogger(ApiTestingServiceNew1.class);
    private static final int MIN_VOLUME_THRESHOLD = 10000;
    private static final int CHUNK_SIZE = 3;

    @Autowired
    private IOPulseService ioPulseService;

    @Value("${endTime}")
    private String endTime;
    @Value("${date}")
    private String date;
    public List<StockResponse> callApi() {
        List<String> stockList = readStockList("src/main/resources/stocksList.txt");
        if (stockList == null) return null;

        return stockList.parallelStream()
                .map(this::processStock)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> readStockList(String filePath) {
        try {
            return Files.lines(Paths.get(filePath)).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error reading stock list: {}", e.getMessage());
            return null;
        }
    }

    private StockResponse processStock(String stock) {
        try {
            String selectedDate = (date != null && !date.isEmpty()) ? date : LocalDate.now().toString();

            ResponseEntity<ApiResponse> response = ioPulseService.sendRequest(stock, selectedDate);
            ApiResponse apiResponse = response.getBody();

            if (apiResponse == null || apiResponse.getData().size() <= CHUNK_SIZE) {
                log.info("Insufficient data for stock: {}", stock);
                return null;
            }

            if (apiResponse.getData().get(0).getTradedVolume() < MIN_VOLUME_THRESHOLD) {
                return null;
            }

            return processApiResponse(apiResponse, stock);
        } catch (Exception e) {
            log.error("Error processing stock {}: {}", stock, e.getMessage());
            return null;
        }
    }

    private StockResponse processApiResponse(ApiResponse apiResponse, String stock) {
        List<List<ApiResponse.Data>> chunks = chunkByMinutes(apiResponse.getData());
        if (chunks.size() < CHUNK_SIZE) {
            log.info("Insufficient chunks for stock: {}", stock);
            return null;
        }

        List<ApiResponse.Data> firstChunk = chunks.get(1);
        double firstCandleHigh = firstChunk.stream().mapToDouble(ApiResponse.Data::getHigh).max().orElse(0.0);
        double firstCandleLow = firstChunk.stream().mapToDouble(ApiResponse.Data::getLow).min().orElse(0.0);

        ApiResponse.Data previousData = firstChunk.get(firstChunk.size() - 1);

        for (int i = 2; i < chunks.size() - 1; i++) {
            StockResponse response = evaluateChunk(chunks.get(i), previousData, firstCandleHigh, firstCandleLow, stock);
            if (response != null) return response;
            previousData = chunks.get(i).get(chunks.get(i).size() - 1);
        }
        return null;
    }

    private StockResponse evaluateChunk(List<ApiResponse.Data> chunk, ApiResponse.Data previousData, double firstCandleHigh, double firstCandleLow, String stock) {
        long totalVolume = chunk.stream().mapToLong(ApiResponse.Data::getTradedVolume).sum();
        if (totalVolume < MIN_VOLUME_THRESHOLD) return null;

        ApiResponse.Data recentData = chunk.get(chunk.size() - 1);
        if (LocalTime.parse(recentData.getTime()).isAfter(LocalTime.parse(endTime))) return null;

        String oiInterpretation = calculateOiInterpretation(recentData, previousData);
        if (!oiInterpretation.contains("BU")) return null;

        if (isShortSetup(firstCandleLow, recentData, oiInterpretation)) {
            return new StockResponse(stock, "N", recentData.getTime(), oiInterpretation, firstCandleHigh, recentData.getClose(), true);
        }

        if (isLongSetup(firstCandleHigh, recentData, oiInterpretation)) {
            return new StockResponse(stock, "P", recentData.getTime(), oiInterpretation, firstCandleLow, recentData.getClose(), true);
        }

        return null;
    }

    private String calculateOiInterpretation(ApiResponse.Data recentData, ApiResponse.Data previousData) {
        double ltpChange = recentData.getClose() - previousData.getClose();
        long oiChange = Long.parseLong(recentData.getOpenInterest()) - Long.parseLong(previousData.getOpenInterest());
        return (oiChange > 0)
                ? (ltpChange > 0 ? "LBU" : "SBU")
                : (ltpChange > 0 ? "SC" : "LU");
    }

    private boolean isShortSetup(double firstCandleLow, ApiResponse.Data recentData, String oiInterpretation) {
        return firstCandleLow > recentData.getClose()
                && recentData.getClose() < recentData.getOpen()
                && "SBU".equals(oiInterpretation);
    }

    private boolean isLongSetup(double firstCandleHigh, ApiResponse.Data recentData, String oiInterpretation) {
        return firstCandleHigh < recentData.getOpen()
                && recentData.getOpen() < recentData.getClose()
                && "LBU".equals(oiInterpretation);
    }

    private List<List<ApiResponse.Data>> chunkByMinutes(List<ApiResponse.Data> dataList) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime startTime = LocalTime.of(9, 16, 0);

        return new ArrayList<>(dataList.stream()
                .collect(Collectors.groupingBy(
                        data -> {
                            LocalTime time = LocalTime.parse(data.getTime(), timeFormatter);
                            int minutesSinceStart = (int) java.time.Duration.between(startTime, time).toMinutes();
                            int minuteBucket = (minutesSinceStart / CHUNK_SIZE) * CHUNK_SIZE;
                            return startTime.plusMinutes(minuteBucket).withSecond(0);
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                )).values());
    }
}