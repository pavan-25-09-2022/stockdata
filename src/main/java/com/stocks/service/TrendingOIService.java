package com.stocks.service;

import com.stocks.dto.*;
import com.stocks.entity.TrendingOiEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrendingOIService {

    @Autowired
    IOPulseService ioPulseService;

    @Autowired
    CommonValidation commonValidation;

    @Autowired
    FutureAnalysisService futureAnalysisService;

    private static final Logger log = LoggerFactory.getLogger(TrendingOIService.class);

    public List<TrendingOiEntity> fetchTrendingOIData(Properties properties) {
        // Logic to fetch trending OI data from an external API or database
        TrendingOiResponse apiResponse = ioPulseService.getTrendingOI(properties);
        if (apiResponse == null || apiResponse.getData() == null ||
                apiResponse.getData().getData() == null || apiResponse.getData().getData().isEmpty()) {
            log.info("Data size is less than or equal to 6 for stock: " + properties.getStockName());
            return null;
        }
        Map<String, Map<String, FutureAnalysis>> stringMapMap = futureAnalysisService.futureAnalysis(properties);
        Map<String, FutureAnalysis> futureAnalysisMap = stringMapMap.get(properties.getStockName());

        Map<String, TrendingOiEntity> stringTrendingOiEntityMap = processTrendingOiResponse(apiResponse, properties, properties.getStockName());
        List<String> shortCoveingDuration = new ArrayList<>();


        return stringTrendingOiEntityMap.values().stream().toList();


    }

    private Map<String, TrendingOiEntity> processTrendingOiResponse(TrendingOiResponse trendingOiResponse, Properties properties, String stock) {
        List<TrendingOiData> dataList = trendingOiResponse.getData().getData();
        if (dataList == null || dataList.isEmpty()) {
            log.info("No data available for stock: " + stock);
            return null;
        }

        Map<String, TrendingOiEntity> trendingOiMap = new LinkedHashMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        List<List<TrendingOiData>> chunks = commonValidation.chunkByMinutesForTrendingOI(dataList, properties.getInterval());

        if (chunks.size() < 3) {
            log.info("Insufficient data chunks for stock: " + stock);
            return null;
        }
        TrendingOiData previousEodData = null;
        TrendingOiEntity previousEntry = null;
        double previousDiffInOi = 0;
        for (int i = 0; i < chunks.size(); i++) {
            List<TrendingOiData> chunk = chunks.get(i);
            if (chunk.isEmpty()) {
                continue;
            }

            if (chunk.size() == 1 && previousEodData == null) {
                previousEodData = chunk.get(0);
                previousEodData.setStTime("09:15");
                continue;
            }

            double curHigh = 0.0;
            double curLow = 0.0;
            double curOpen = 0.0;
            double curClose = 0.0;
            String startTime = null;
            for (TrendingOiData data : chunk) {

                if (startTime == null) {
                    LocalTime time = LocalTime.parse(data.getStTime(), timeFormatter).minusMinutes(1);
                    for(int j=1;j < properties.getInterval(); j++) {
                        int zero = time.getMinute() % properties.getInterval();
                        if(zero == 0){
                            break;
                        }
                        time = time.minusMinutes(1);
                    }
                    startTime = time.toString();
                }

                if (curOpen == 0.0) {
                    curOpen = previousEntry == null ? data.getInClose() : previousEntry.getClose();
                }
                curClose = data.getInClose();
                if (curHigh == 0.0) {
                    curHigh = data.getInHigh();
                }
                if (curLow == 0.0) {
                    curLow = data.getInLow();
                }
                curHigh = Math.max(data.getInHigh(), curHigh);
                curLow = Math.min(data.getInLow(), curLow);
            }

            TrendingOiData lastData = chunk.get(chunk.size() - 1);

            LocalTime endTime = LocalTime.parse(lastData.getStTime(), timeFormatter);
            if(endTime.getMinute() % properties.getInterval() !=0){
                for(int j=1;j < properties.getInterval(); j++) {
                    int zero = endTime.getMinute() % properties.getInterval();
                    if(zero == 0){
                        break;
                    }
                    endTime = endTime.plusMinutes(1);
                }
            }

            double callOiChange = lastData.getTotalCeOi() - previousEodData.getTotalCeOi();
            double putOiChange = lastData.getTotalPeOi() - previousEodData.getTotalPeOi();
            double diffInOi = putOiChange - callOiChange;
            String duration = startTime + "-" + endTime;

            TrendingOiEntity trendingOiEntity = new TrendingOiEntity();
            trendingOiEntity.setStockSymbol(stock);
            trendingOiEntity.setStockDate(properties.getStockDate());
            trendingOiEntity.setFetchTime(duration);
            trendingOiEntity.setClose(curClose);
            trendingOiEntity.setHigh(curHigh);
            trendingOiEntity.setLow(curLow);
            trendingOiEntity.setOpen(curOpen);
            trendingOiEntity.setChangeInCallOi(callOiChange);
            trendingOiEntity.setChangeInPutOi(putOiChange);
            trendingOiEntity.setDiffInOi(diffInOi);
            trendingOiEntity.setChangeInDirectionOi(diffInOi - previousDiffInOi);
            trendingOiEntity.setNetPcr((double) (putOiChange / callOiChange));
            trendingOiEntity.setSentiment(diffInOi > 0 ? "Bullish" : (diffInOi < 0 ? "Bearish" : "Neutral"));
            //trendingOiData.setDirectionOfChangePercentage((ltpChange / firstData.getLtp()) * 100);

            if (previousEntry != null) {
                trendingOiEntity.setPEShorted(trendingOiEntity.getChangeInPutOi() < previousEntry.getChangeInPutOi());
                trendingOiEntity.setCEShorted(trendingOiEntity.getChangeInCallOi() < previousEntry.getChangeInCallOi());
            }
            trendingOiMap.put(duration, trendingOiEntity);
           // previousEodData = lastData;
            previousEntry = trendingOiEntity;
            previousDiffInOi = diffInOi;
        }

        return trendingOiMap;
    }


}
