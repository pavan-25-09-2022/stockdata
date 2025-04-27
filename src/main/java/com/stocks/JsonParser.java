package com.stocks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class JsonParser {
    public static void main(String[] args) throws Exception {
        File jsonFile = new File(JsonParser.class.getClassLoader().getResource("stocks.json").getFile());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonFile);

        // Assuming the JSON is an array, get the first element and extract the "text" field
        for (int i = 0; i < rootNode.size(); i++) {
            String node = rootNode.get(i).get("text").asText();
            System.out.println(node);
        }
    }
}