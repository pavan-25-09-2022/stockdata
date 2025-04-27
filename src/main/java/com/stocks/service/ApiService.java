package com.stocks.service;

import com.stocks.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Deprecated
public class ApiService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.auth.token}")
    private String authToken;

    @Value("${date}")
    private String date;
    private static final Logger log = LoggerFactory.getLogger(ApiService.class);


    public List<String> callApi() {
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Content-Type", "application/json");
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
        return stockList.parallelStream().map(stock -> {
            try {
                // Create payload
                Map<String, String> payload = new HashMap<>();
                payload.put("stSelectedFutures", stock);
                payload.put("stSelectedExpiry", "I");
                payload.put("stSelectedAvailableDate", selectedDate);
                payload.put("stSelectedModeOfData", "live");

                // Create request entity
                HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

                // Make POST request
                ResponseEntity<ApiResponse> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, ApiResponse.class);

                // Process response
                ApiResponse apiResponse = response.getBody();
                if (apiResponse == null || apiResponse.getData().size() <= 6) {
                    log.info("Data size is less than or equal to 6 for stock: " + stock);
                    return null;
                }

                return processApiResponse(apiResponse, stock);
            } catch (Exception e) {
                log.error("Error processing stock: " + stock + ", " + e.getMessage());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private String processApiResponse(ApiResponse apiResponse, String stock) {
        List<ApiResponse.Data> list = apiResponse.getData();
        int chunkSize = 5;

        List<Long> volumes = new ArrayList<>();
        long previousVolume = 0;
        ApiResponse.Data firstCandle = list.get(0);
        volumes.add(firstCandle.getTradedVolume());
        double firstCandleHigh = 0;
        double firstCandleLow = 0;
        for (int i = 1; i < 6; i++) {
            ApiResponse.Data data = list.get(i);
            previousVolume += data.getTradedVolume();
            firstCandleHigh = data.getHigh();
            firstCandleLow = data.getLow();
            firstCandleHigh = Math.max(data.getHigh(), firstCandleHigh);
            firstCandleLow = Math.min(data.getLow(), firstCandleLow);
        }
        volumes.add(previousVolume);

        for (int i = 6; i < list.size(); i += chunkSize) {
            List<ApiResponse.Data> chunk = list.subList(i, Math.min(i + chunkSize, list.size()));
            long totalVolume = 0;
            ApiResponse.Data previousData = list.get(i - 1);
            ApiResponse.Data recentData = null;
            for (ApiResponse.Data data : chunk) {
                totalVolume += data.getTradedVolume();
                recentData = data;
            }
            if (recentData == null) {
                continue;
            }
            double ltpChange = recentData.getClose() - previousData.getClose();
            ltpChange = Double.parseDouble(String.format("%.2f", ltpChange));
            long oiChange = Long.parseLong(recentData.getOpenInterest()) - Long.parseLong(previousData.getOpenInterest());
            String oiInterpretation = (oiChange > 0)
                    ? (ltpChange > 0 ? "Long Build Up" : "Short Build Up")
                    : (ltpChange > 0 ? "Short Covering" : "Long Unwinding");

            long finalTotalVolume = totalVolume;
            boolean isHigher = volumes.stream().anyMatch(volume -> finalTotalVolume > volume);

            if (firstCandleLow > recentData.getClose() && isHigher) {
                double stopLoss = firstCandleHigh;
                return "NS " + stock + " stopLoss " + stopLoss + " current " + recentData.getClose() + "  " + oiInterpretation + (isHigher ? " volume high" : "") + " @ " + recentData.getTime();
            }
            if (firstCandleHigh < recentData.getClose() && isHigher) {
                double stopLoss = firstCandleLow;
                return "PS " + stock + " stopLoss " + stopLoss + " current " + recentData.getClose() + "  " + oiInterpretation + (isHigher ? " volume high" : "") + " @ " + recentData.getTime();
            }
        }
        return null;
    }
}