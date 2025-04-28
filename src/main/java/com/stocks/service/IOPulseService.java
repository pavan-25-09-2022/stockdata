package com.stocks.service;

import com.stocks.dto.ApiResponse;
import com.stocks.dto.StockEODResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class IOPulseService {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.month.url}")
    private String apiMonthUrl;

    @Value("${api.auth.token}")
    private String authToken;

    @Value("${date}")
    private String date;

    @Value(("${exitTime}"))
    private String exitTime;

    @Value("${endTime}")
    private String endTime;

    ResponseEntity<ApiResponse> sendRequest(String stock, String selectedDate) {
        // Create payload
        Map<String, String> payload = new HashMap<>();
        payload.put("stSelectedFutures", stock);
        payload.put("stSelectedExpiry", "I");
        payload.put("stSelectedAvailableDate", selectedDate);
        payload.put("stSelectedModeOfData", "live");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Content-Type", "application/json");

        // Create request entity
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

        // Make POST request
        ResponseEntity<ApiResponse> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, ApiResponse.class);
        return response;
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
}
