package com.stocks.service;


import com.stocks.dto.ApiResponse;
import com.stocks.dto.FutureAnalysis;
import com.stocks.dto.Properties;
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
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FutureAnalysisService {

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

    @Value("${endTime}")
    private String endTime;

    @Value("${eodValue}")
    private int eodValue;

    private static final Logger log = LoggerFactory.getLogger(FutureAnalysisService.class);


    public Map<String, Map<String, FutureAnalysis>> futureAnalysis(Properties properties) {

        // Path to the file containing the stock list
        String filePath = "src/main/resources/stocksList.txt";

        // Read all lines from the file into a List
        List<String> stockList;
        if (properties.getStockName() != null) {
            stockList = Arrays.asList(properties.getStockName().split(","));
        } else {
            try (java.util.stream.Stream<String> lines = Files.lines(Paths.get(filePath))) {
                stockList = lines.collect(Collectors.toList());
            } catch (IOException e) {
                log.error("Error reading file: " + e.getMessage());
                return null;
            }
        }

        Map<String, Map<String, FutureAnalysis>> futureAnalysisStocks = new HashMap<>();

        // Process stocks in parallel
        long count = stockList.parallelStream().map(stock -> {
            try {
                // Make POST request
                ResponseEntity<ApiResponse> response = ioPulseService.sendRequest(properties, stock);

                // Process response
                ApiResponse apiResponse = response.getBody();
                if (apiResponse == null || apiResponse.getData().size() <= 6) {
                    log.info("Data size is less than or equal to 6 for stock: " + stock);
                    return null;
                }

                Map<String, FutureAnalysis> futureAnalysisMap = processFutureAnalysisResponse(apiResponse, properties, stock);
                if (futureAnalysisMap != null) {
                    futureAnalysisStocks.put(stock, futureAnalysisMap);
                }
                return futureAnalysisMap;
            } catch (Exception e) {
                log.error("Error processing stock: " + stock + ", " + e.getMessage(), e);
                return null;
            }
        }).filter(Objects::nonNull).count();

        return futureAnalysisStocks;
    }

    public List<List<ApiResponse.Data>> chunkByMinutes(List<ApiResponse.Data> dataList, int minutes) {

        if (dataList == null || dataList.isEmpty()) {
            log.warn("Data list is null or empty, returning an empty list.");
            return Collections.emptyList();
        }

        int finalMinutes = minutes != 0 ? minutes : MINS;

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Group data by intervals
        Map<LocalTime, List<ApiResponse.Data>> groupedData = dataList.stream()
                .collect(Collectors.groupingBy(
                        data -> {
                            LocalTime time = LocalTime.parse(data.getTime(), timeFormatter);
                            int minutesSinceStart = (int) java.time.Duration.between(START_TIME, time).toMinutes();
                            int minuteBucket = (minutesSinceStart / finalMinutes) * finalMinutes;
                            return START_TIME.plusMinutes(minuteBucket).withSecond(0);
                        },
                        LinkedHashMap::new, // Maintain order
                        Collectors.toList()
                ));
        return new ArrayList<>(groupedData.values());
    }

    private Map<String, FutureAnalysis> processFutureAnalysisResponse(ApiResponse apiResponse, Properties properties, String stock) {
        List<ApiResponse.Data> list = apiResponse.getData();
        ApiResponse.Data previousData;
        long previousVolume = 0;
        double firstCandleHigh = 0.0;
        double firstCandleLow = 0.0;
        Map<String, FutureAnalysis> futureAnalysisMap = new LinkedHashMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        List<List<ApiResponse.Data>> chunks = chunkByMinutes(list, properties.getInterval());
        if (chunks.size() < 3) {
            log.info("Data size is less than 3 for stock: " + stock);
            return null;
        }

        List<ApiResponse.Data> firstCandleChunk = chunks.get(1);
        for (ApiResponse.Data data : firstCandleChunk) {
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

        long highVolume = previousVolume;
        previousData = firstCandleChunk.get(firstCandleChunk.size() - 1);
        for (int i = 2; i < chunks.size() - 1; i++) {
            List<ApiResponse.Data> chunk = chunks.get(i);
            long totalVolume = 0;
            ApiResponse.Data recentData = null;
            ApiResponse.Data firstCandle = null;

            double curOpen = 0.0;
            double curClose = 0.0;
            double curHigh = 0.0;
            double curLow = 0.0;
            long specificHighVolume = 0;
            String highVolumeCandle = null;
            String startTime = null;
            for (ApiResponse.Data data : chunk) {
                totalVolume += data.getTradedVolume();
                recentData = data;
                if (startTime == null) {
                    LocalTime time = LocalTime.parse(data.getTime(), timeFormatter).minusMinutes(1);
                    startTime = time.toString();
                }
                if (specificHighVolume == 0.0) {
                    specificHighVolume = data.getTradedVolume();
                    highVolumeCandle = data.getTime();
                }
                if (data.getTradedVolume() > specificHighVolume) {
                    specificHighVolume = data.getTradedVolume();
                    highVolumeCandle = data.getTime();
                }
                if (firstCandle == null) {
                    firstCandle = data;
                }
                if (curOpen == 0.0) {
                    curOpen = data.getOpen();
                }
                if (curClose == 0.0) {
                    curClose = data.getClose();
                }
                if (curHigh == 0.0) {
                    curHigh = data.getHigh();
                }
                if (curLow == 0.0) {
                    curLow = data.getLow();
                }
                curHigh = Math.max(data.getHigh(), curHigh);
                curLow = Math.min(data.getLow(), curLow);
                curOpen = Math.min(data.getOpen(), curOpen);
                curClose = Math.min(data.getClose(), curClose);
            }

            if (recentData == null) {
                continue;
            }

            double ltpChange = recentData.getClose() - previousData.getClose();
            ltpChange = Double.parseDouble(String.format("%.2f", ltpChange));
            long oiChange = Long.parseLong(recentData.getOpenInterest()) - Long.parseLong(previousData.getOpenInterest());
            long totalOiChange = Long.parseLong(previousData.getOpenInterest()) + oiChange;
            String oiInterpretation = (oiChange > 0)
                    ? (ltpChange > 0 ? "LBU" : "SBU")
                    : (ltpChange > 0 ? "SC" : "LU");

            boolean isHigher = false;
            if (highVolume < totalVolume) {
                isHigher = true;
                highVolume = totalVolume;
            }


            previousData = chunk.get(chunk.size() - 1);

            String duration = startTime + "-" + FormatUtil.formatTime(previousData.getTime());
            double strength = 0.0;
            if(oiChange != 0) {
                DecimalFormat df = new DecimalFormat("#.####");
                strength = Math.abs(Double.parseDouble(df.format(ltpChange / oiChange)));
            }

            FutureAnalysis futureAnalysis = new FutureAnalysis(duration, (double) totalOiChange, 0.0, previousData.getDayHigh(), previousData.getDayLow(), curClose, curHigh, curLow, curOpen, oiChange, oiInterpretation, "", ltpChange, totalVolume, isHigher,strength);
            futureAnalysisMap.put(duration, futureAnalysis);

        }
        System.out.println("FutureAnalyis");
        for (Map.Entry<String, FutureAnalysis> map : futureAnalysisMap.entrySet()) {
            System.out.println("Key " + map.getKey() + "  Value " + map.getValue());
        }
        return futureAnalysisMap;
    }

}
