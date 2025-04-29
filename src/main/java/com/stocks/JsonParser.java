package com.stocks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonParser {
    public static void main(String[] args) throws Exception {
        marketHolidays();
    }
    public static void getStocks() throws IOException {
        File jsonFile = new File(JsonParser.class.getClassLoader().getResource("stocks.json").getFile());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonFile);

        // Assuming the JSON is an array, get the first element and extract the "text" field
        for (int i = 0; i < rootNode.size(); i++) {
            String node = rootNode.get(i).get("text").asText();
            System.out.println(node);
        }
    }
    public static void marketHolidays(){
        try {
            // Load the JSON file
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File("src/main/resources/holiday.txt"));

            // Extract the "data" array
            JsonNode dataArray = rootNode.get("data");
            List<String> stDates = new ArrayList<>();

            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode holiday : dataArray) {
                    // Extract "stDate" from each object
                    String stDate = holiday.get("stDate").asText();
                    stDates.add(stDate);
                }
            }

            // Print the extracted dates
            stDates.forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}