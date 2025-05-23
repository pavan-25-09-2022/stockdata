package com.stocks.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocks.dto.*;
import com.stocks.utils.FormatUtil;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class IOPulseService {

    private static final Logger log = LoggerFactory.getLogger(IOPulseService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.month.url}")
    private String apiMonthUrl;

    @Value("${api.option.chain.url}")
    private String apiOptionChainUrl;

    @Value("${api.market.movers}")
    private String apiMarketMoversUrl;

    @Value("${api.auth.token}")
    private String authToken;

    @Value(("${exitTime}"))
    private String exitTime;

    @Value("${endTime}")
    private String endTime;

    ApiResponse sendRequest(Properties properties, String stock) {
        try{
        // Create payload

        String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() : LocalDate.now().toString();
        String workingDay = MarketHolidayUtils.getWorkingDay(selectedDate);
        Map<String, String> payload = new HashMap<>();
        payload.put("stSelectedFutures", stock);
        payload.put("stSelectedExpiry", "I");
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
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);
            if (response.getBody() != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                ApiResponse apiResponse = objectMapper.readValue(response.getBody(), ApiResponse.class);
                // Now use apiResponse as needed
                return apiResponse;
            }

        }catch (Exception e){
            log.error("Error in sendRequest", e);
        }
        return null;
    }

    StockEODResponse getMonthlyData(String stock) {
        // Create payload
        Map<String, String> payload = new HashMap<>();
        payload.put("stSelectedFutures", stock);
        payload.put("stSelectedExpiry", "I");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Content-Type", "application/json");

        // Create request entity
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

        // Make POST request
        ResponseEntity<StockEODResponse> response = restTemplate.exchange(apiMonthUrl, HttpMethod.POST, requestEntity, StockEODResponse.class);
        return response.getBody();
    }

    public OptionChainResponse getOptionChain(Properties properties, String stock, LocalTime startTime) {
        // Create payload
        Map<String, String> payload = new HashMap<>();

        String startStringTime = startTime != null ? startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "09:15:00";
        payload.put("stSelectedOptions", stock);
        String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() : LocalDate.now().toString();

        payload.put("stSelectedAvailableDate", selectedDate);
        payload.put("stSelectedAvailableExpiryDate", properties.getExpiryDate());
        LocalDate selected = LocalDate.parse(selectedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate today = LocalDate.now();
        if (selected.isBefore(today)) {
            payload.put("stSelectedModeOfData", "historical");
        } else {
            payload.put("stSelectedModeOfData", "live");
        }
        payload.put("inSelectedavailableTimeRange", "CUSTOM_TIME");
        payload.put("stStartTime", startStringTime);
        payload.put("stEndTime", FormatUtil.formatTimeHHmmss(FormatUtil.getTime(startStringTime, properties.getInterval())));


        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Content-Type", "application/json");

        // Create request entity
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

        // Make POST request
        try{
        ResponseEntity<OptionChainResponse> response = restTemplate.exchange(apiOptionChainUrl, HttpMethod.POST, requestEntity, OptionChainResponse.class);
        return response.getBody();
        }catch (Exception e){
           log.error("Error in getOptionChain", e);
            return null;
        }
    }

    public void marketMovers(Properties properties){
        try{
            // Create payload

            String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() :  LocalDate.now().toString();
            Map<String, String> payload = new HashMap<>();
            payload.put("stSelectedExpiry", "I");
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
            ResponseEntity<ApiResponse> response = restTemplate.exchange(apiMarketMoversUrl, HttpMethod.POST, requestEntity, ApiResponse.class);
            if (response.getBody() != null) {
//            log.error("Response msg for stock: "+ stock +"--- " + response.getBody().getMsg());
//                return response;ejhgcbruejkreldglhcrktutfluljvvkleguetvcknuv
            }
//            return response;
        }catch (Exception e){
            log.error("Error in sendRequest", e);
//            return null;
        }
    }
}
