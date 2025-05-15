package com.stocks.service;

import com.stocks.dto.OpenHighLowResponse;
import com.stocks.dto.Properties;
import com.stocks.utils.MarketHolidayUtils;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OpenHighLowService {


    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.day.high.low.url}")
    private String apiUrl;

    @Value("${api.auth.token}")
    private String authToken;

    @Value("${date}")
    private String date;


    private static final Logger log = LoggerFactory.getLogger(DayHighLowService.class);


    public List<String> dayHighLow(Properties properties) {

        // Path to the file containing the stock list
        String filePath = "src/main/resources/stocksList.txt";

        // Read all lines from the file into a List
        List<String> stockList;
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            stockList = lines.collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error reading file: " + e.getMessage());
            return null;
        }
        List<String> dayHighLowStocks = new ArrayList<>();
        List<String> notDayHighStocks = new ArrayList<>();
        try {
            ResponseEntity<OpenHighLowResponse> response = sendRequest(properties);
            OpenHighLowResponse apiResponse = response.getBody();
            List<OpenHighLowResponse.Data> data = apiResponse.getData();
            for (OpenHighLowResponse.Data stock : data) {
                dayHighLowStocks.add(stock.getSymbol());
            }

        } catch (Exception e) {
            return null;
        }

        for(String stock : stockList){
            if(!dayHighLowStocks.contains(stock)){
                notDayHighStocks.add(stock);
            }
        }

        List<String> previousDayHighLowStocks = new ArrayList<>();
        properties.setStockDate("2025-05-08");
        ResponseEntity<OpenHighLowResponse> response = sendRequest(properties);
        OpenHighLowResponse apiResponse = response.getBody();
        List<OpenHighLowResponse.Data> data = apiResponse.getData();
        for (OpenHighLowResponse.Data stock : data) {
            previousDayHighLowStocks.add(stock.getSymbol());
        }

        List<String> presentInPreviousDayStock = new ArrayList<>();
        for(String string :notDayHighStocks ){
            if(previousDayHighLowStocks.contains(string)){
                presentInPreviousDayStock.add(string);
            }
        }
        return presentInPreviousDayStock;
    }


    ResponseEntity<OpenHighLowResponse> sendRequest(Properties properties) {
        // Create payload

        String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() : date != null ? date : LocalDate.now().toString();
        Map<String, String> payload = new HashMap<>();
        payload.put("stSelectedAsset", null);
        payload.put("stSelectedAvailableDate", selectedDate);
        // Determine if the selected date is in the past
        LocalDate selected = LocalDate.parse(selectedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate today = LocalDate.now();
        if (selected.isBefore(today)) {
            payload.put("stSelectedModeOfData", "historical");
        } else {
            payload.put("stSelectedModeOfData", "live");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Content-Type", "application/json");

        // Create request entity
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

        // Make POST request
        ResponseEntity<OpenHighLowResponse> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, OpenHighLowResponse.class);
        return response;
    }

}
