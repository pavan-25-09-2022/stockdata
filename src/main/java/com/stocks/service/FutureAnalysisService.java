package com.stocks.service;


import com.stocks.dto.ApiResponse;
import com.stocks.dto.FutureAnalysis;
import com.stocks.dto.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private static final LocalTime MARKET_START_TIME = LocalTime.of(9,15,0);
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
        if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
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
        long count = stockList.stream().map(stock -> {
            try {
                // Make POST request
                ApiResponse apiResponse= ioPulseService.sendRequest(properties, stock);

                // Process response
//                ApiResponse apiResponse = response.getBody();
                if (apiResponse == null ||apiResponse.getData()==null || apiResponse.getData().size() <= 6) {
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
        ApiResponse.Data previousEodChunk = chunks.get(0).get(0);
        double dayHigh = 0.0;
        double dayLow = 0.0;
        List<ApiResponse.Data> firstCandleChunk = chunks.get(1);
        for (ApiResponse.Data data : firstCandleChunk) {
            previousVolume += data.getTradedVolume();
            if (firstCandleHigh == 0.0) {
                firstCandleHigh = data.getHigh();
                dayHigh = data.getHigh();
            }
            if (firstCandleLow == 0.0) {
                firstCandleLow = data.getLow();
                dayLow = data.getLow();
            }
            firstCandleHigh = Math.max(data.getHigh(), firstCandleHigh);
            firstCandleLow = Math.min(data.getLow(), firstCandleLow);
            dayHigh = Math.max(data.getHigh(), dayHigh);
            dayLow = Math.min(data.getLow(), dayLow);

        }
        long highVolume = previousVolume;
        ApiResponse.Data firstCandleOfADay = firstCandleChunk.get(0);
        previousData = firstCandleChunk.get(firstCandleChunk.size() - 1);
        double firstCandleLtpChange = previousData.getClose() - previousEodChunk.getClose();
        firstCandleLtpChange = Double.parseDouble(String.format("%.2f", firstCandleLtpChange));
        long firstCandleOiChange = Long.parseLong(previousData.getOpenInterest()) - Long.parseLong(previousEodChunk.getOpenInterest());
        long firstCandleTotalOiChange = Long.parseLong(previousEodChunk.getOpenInterest()) + firstCandleOiChange;
        String firstCandleOiInterpretation = (firstCandleOiChange > 0)
                ? (firstCandleLtpChange > 0 ? "LBU" : "SBU")
                : (firstCandleLtpChange > 0 ? "SC" : "LU");
        double firstCandleStrength = 0.0;
        if(firstCandleOiChange != 0) {
            DecimalFormat df = new DecimalFormat("#.####");
            firstCandleStrength = Math.abs(Double.parseDouble(df.format(firstCandleLtpChange / firstCandleOiChange)));
        }
        String firstCandleDuration = MARKET_START_TIME+"-"+MARKET_START_TIME.plusMinutes(properties.getInterval());
        double firstCandlePercentageChange = ((double) firstCandleOiChange /Long.parseLong(previousEodChunk.getOpenInterest()))*100;
        double firstCandleLtpChangePercentage = (firstCandleLtpChange / previousEodChunk.getClose()) * 100;
        long firstCandleTotalDayChangeInOI = firstCandleOiChange - Long.parseLong(previousEodChunk.getOpenInterest());
        FutureAnalysis firstCandleFutureAnalysis = new FutureAnalysis(stock, properties.getStockDate(), firstCandleDuration, (double) firstCandleTotalOiChange, firstCandleTotalDayChangeInOI, previousData.getDayHigh(), previousData.getDayLow(), previousData.getClose(), firstCandleHigh, firstCandleLow, firstCandleOfADay.getOpen(), firstCandleOiChange, firstCandleOiInterpretation, "", firstCandleLtpChange, highVolume, false,firstCandleStrength, firstCandlePercentageChange, firstCandleLtpChangePercentage);
        futureAnalysisMap.put(firstCandleDuration, firstCandleFutureAnalysis);
        for (int i = 2; i < chunks.size() - 1; i++) {
            List<ApiResponse.Data> chunk = chunks.get(i);
            long totalVolume = 0;
            ApiResponse.Data recentData = null;
            ApiResponse.Data firstCandle = null;

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
                    for(int j=1;j < properties.getInterval(); j++) {
                        int zero = time.getMinute() % properties.getInterval();
                        if(zero == 0){
                            break;
                        }
                        time = time.minusMinutes(1);
                    }
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
                if (curHigh == 0.0) {
                    curHigh = data.getHigh();
                }
                if (curLow == 0.0) {
                    curLow = data.getLow();
                }
                curHigh = Math.max(data.getHigh(), curHigh);
                curLow = Math.min(data.getLow(), curLow);

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

            boolean isDayHigh = false;
            boolean isDayLow = false;
            if (dayHigh < curHigh) {
                isDayHigh = true;
                dayHigh = curHigh;
            }
            if (curLow < dayLow) {
                isDayLow = true;
                dayLow = curLow;
            }

            String isLevelBreak =  isDayHigh ? "D.H.B" : isDayLow ? "D.L.B" : "";


            previousData = chunk.get(chunk.size() - 1);
            // match the key interval if data is missing
            LocalTime endTime = LocalTime.parse(previousData.getTime(), timeFormatter);
            if(endTime.getMinute() % properties.getInterval() !=0){
                for(int j=1;j < properties.getInterval(); j++) {
                    int zero = endTime.getMinute() % properties.getInterval();
                    if(zero == 0){
                        break;
                    }
                    endTime = endTime.plusMinutes(1);
                }
            }

            String duration = startTime + "-" + endTime.toString();
            double strength = 0.0;
            if(oiChange != 0) {
                DecimalFormat df = new DecimalFormat("#.####");
                strength = Math.abs(Double.parseDouble(df.format(ltpChange / oiChange)));
            }

            double oiPercentageChange = ((double) oiChange /Long.parseLong(previousData.getOpenInterest()))*100;

            double ltpPercentageChange = (ltpChange / previousData.getClose()) * 100;

            long totalDayChangeInOI = totalOiChange - Long.parseLong(previousEodChunk.getOpenInterest());

            FutureAnalysis futureAnalysis = new FutureAnalysis(stock, properties.getStockDate(), duration, (double) totalOiChange, totalDayChangeInOI, previousData.getDayHigh(), previousData.getDayLow(), recentData.getClose(), curHigh, curLow, firstCandle.getOpen(), oiChange, oiInterpretation, isLevelBreak, ltpChange, totalVolume, isHigher,strength, oiPercentageChange, ltpPercentageChange);
            futureAnalysisMap.put(duration, futureAnalysis);

        }
        return futureAnalysisMap;
    }

}
