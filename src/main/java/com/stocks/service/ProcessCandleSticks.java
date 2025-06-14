package com.stocks.service;

import com.stocks.dto.ApiResponse;
import com.stocks.dto.Candle;
import com.stocks.dto.Properties;
import com.stocks.dto.StockResponse;
import com.stocks.utils.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProcessCandleSticks {

    private static final Logger log = LoggerFactory.getLogger(ProcessCandleSticks.class);

    @Autowired
    private StockDataManager stockDataManager;
    @Autowired
    private RSICalculator rsiCalculator;
    @Autowired
    private CalculateOptionChain calculateOptionChain;
    @Autowired
    private IOPulseService ioPulseService;
    @Autowired
    private CommonValidation commonValidation;

    public StockResponse processApiResponses(Properties properties, String stock) {
        // Make POST request
        ApiResponse apiResponse = ioPulseService.sendRequest(properties, stock);

        // Process response
//        ApiResponse apiResponse = response.getBody();
        if (apiResponse == null || apiResponse.getData() == null || apiResponse.getData().size() <= 5) {
            log.info("Data size is less than or equal to 6 for stock: " + stock);
            return null;
        }
        List<ApiResponse.Data> list = apiResponse.getData();
        List<Double> rsiList = new ArrayList<>();
        ApiResponse.Data previousData;
        long fistCandleVol = 0;
//        List<Long> volumes = new ArrayList<>();
        long previousVolume = 0;
        double firstCandleHigh = 0.0;
        double firstCandleLow = 0.0;
        List<List<ApiResponse.Data>> chunks = commonValidation.chunkByMinutes(list, properties.getInterval());
        if (chunks.size() < 3) {
            log.info("Data size is less than 3 for stock: " + stock);
            return null;
        }
        ApiResponse.Data yestCandle = list.get(0);
        List<ApiResponse.Data> firstCandleChunk = chunks.get(1);
//        if(firstCandleChunk.size() < 3){
//            return null;
//        }
        double firstCandleClose = 0.0;
        double firstCandleOpen =0.0;
        for (ApiResponse.Data data : firstCandleChunk) {
            previousVolume += data.getTradedVolume();
            if (firstCandleHigh == 0.0) {
                firstCandleHigh = data.getHigh();
            }
            if (firstCandleLow == 0.0) {
                firstCandleLow = data.getLow();
            }
            firstCandleClose = data.getClose();
            if(firstCandleOpen == 0.0) {
                firstCandleOpen = data.getOpen();
            }
            firstCandleHigh = Math.max(data.getHigh(), firstCandleHigh);
            firstCandleLow = Math.min(data.getLow(), firstCandleLow);
        }
        Candle firstCandleStick = new Candle();
        firstCandleStick.setHigh(firstCandleHigh);
        firstCandleStick.setLow(firstCandleLow);
        firstCandleStick.setClose(firstCandleClose);
        firstCandleStick.setOpen(firstCandleOpen);
        firstCandleStick.setVolume(previousVolume);
        rsiList.add(firstCandleClose);

        fistCandleVol = previousVolume;
        String recentTime = stockDataManager.getRecentTime(stock, FormatUtil.getCurDate(properties));
        LocalTime recentTimeStamp = null;
        if (recentTime != null) {
            recentTimeStamp = LocalTime.parse(recentTime);
        }

        long highVolume = previousVolume;
        previousData = firstCandleChunk.get(firstCandleChunk.size() - 1);
        Map<Long, Double> map = new HashMap<>();
        double prevChgeInPer = 0.0;
        StockResponse res = null;
        for (int i = 2; i < chunks.size() - 1; i++) {
            List<ApiResponse.Data> chunk = chunks.get(i);
            long totalVolume = 0;
            ApiResponse.Data recentData = null;
            ApiResponse.Data firstCandle = null;

            double curHigh = 0.0;
            double curLow = 0.0;
            double curOpen= 0.0;
            double curClose = 0.0;
            long specificHighVolume = 0;
            String highVolumeCandle = null;
            for (ApiResponse.Data data : chunk) {
                totalVolume += data.getTradedVolume();
                recentData = data;
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
                if( curOpen == 0.0) {
                    curOpen = data.getOpen();
                }
                curClose = data.getClose();
                if (curHigh == 0.0) {
                    curHigh = data.getHigh();
                }
                if (curLow == 0.0) {
                    curLow = data.getLow();
                }
                curHigh = Math.max(data.getHigh(), curHigh);
                curLow = Math.min(data.getLow(), curLow);
            }
            Candle candle = new Candle();
            candle.setHigh(curHigh);
            candle.setLow(curLow);
            candle.setClose(curClose);
            candle.setOpen(curOpen);
            candle.setVolume(totalVolume);

//            double curOpen = chunk.get(0).getOpen();
//            double curClose = chunk.get(chunk.size() - 1).getClose();
            if (recentData == null) {
                continue;
            }
            rsiList.add(recentData.getClose());
            if (rsiList.size() > 14) {
                rsiList.remove(0); // Remove the oldest element
            }
            LocalTime localTime = LocalTime.parse(recentData.getTime());
//            LocalTime endLocalTime = LocalTime.parse(endTime);
//            if (localTime.isAfter(endLocalTime)) {
//                return null;
//            }
            double ltpChange = recentData.getClose() - previousData.getClose();
            ltpChange = Double.parseDouble(String.format("%.2f", ltpChange));
            long oiChange = Long.parseLong(recentData.getOpenInterest()) - Long.parseLong(previousData.getOpenInterest());
            String oiInterpretation = (oiChange > 0)
                    ? (ltpChange > 0 ? "LBU" : "SBU")
                    : (ltpChange > 0 ? "SC" : "LU");

            boolean isHigher = false;
            if (highVolume < totalVolume) {
                isHigher = true;
                highVolume = totalVolume;
            }
//            if (!oiInterpretation.contains("BU")) {
//                return null;
//            }
            previousData = chunk.get(chunk.size() - 1);
//            if (recentTimeStamp != null) {
//                if (localTime.isBefore(recentTimeStamp)) {
//                    continue;
//                }
//            }
            if (chunk.size() < 3) {
                continue;
            }
//            if (curClose < 2000) {
//                return null;
//            }
            double val1 = ((curHigh - curLow) / curLow) * 100;
            double chgeInPer = Math.abs(val1);
            double firstCandleChgeInPer = (((firstCandleClose- firstCandleOpen ) / firstCandleOpen ) * 100);
            double chgeWithYstd = (((firstCandleClose- yestCandle.getClose() ) / yestCandle.getClose() ) * 100);
            double highToOpenChge = (((curHigh- curOpen ) / curOpen ) * 100);
            double lowToCloseChge = (((curClose- curLow ) / curLow ) * 100);
//            map.put(totalVolume, Math.abs(chgeInPer));


            double value = rsiCalculator.calculateRSI(rsiList, 14);
//            log.info("RSI value: " + value + " for stock: " + stock + " for period: " + rsiList.size() + " time " + recentData.getTime());
            res = new StockResponse(stock, "", firstCandle.getTime(), recentData.getTime(), oiInterpretation, firstCandleHigh, recentData.getClose(), totalVolume);
            res.setCurLow(curLow);
            res.setCurHigh(curHigh);
            res.setRsi(String.format("%.2f", value));
            res.setChgeInPer(chgeInPer);
            if(isHigher){
                String val = "Y "+ res.getVolume();
                res.setVolume(val);
            }
            res.setCay(String.format("%.2f",chgeWithYstd) +" " + String.format("%.2f",prevChgeInPer) +" " + String.format("%.2f",val1));
            prevChgeInPer = chgeInPer;
            res.setCurCandle(candle);
            res.setFirstCandle(firstCandleStick);
            if(!"OC".equals(properties.getType()) && isHigher) {
                if (properties.isFetchAll() || recentTimeStamp == null || localTime.isAfter(recentTimeStamp)) {
                    if (recentData.getClose() < firstCandleOpen && lowToCloseChge < 0.4 && curHigh > firstCandleLow && firstCandleChgeInPer <0 && firstCandleChgeInPer>-1.1 &&(oiInterpretation.equals("SBU"))) {
                        res.setStockType("N");
                        res.setStopLoss(firstCandleHigh);
                        res.setLimit(firstCandleLow);
                        return res;
                    }
                    if (recentData.getClose() > firstCandleClose && highToOpenChge < 0.4 && curLow < firstCandleHigh && firstCandleChgeInPer >0  && firstCandleChgeInPer <1.1 && (oiInterpretation.equals("LBU"))) {
                        res.setStockType("P");
                        res.setStopLoss(firstCandleLow);
                        res.setLimit(firstCandleHigh);
                        return res;
                    }
                }
            }
            if(properties.getEndTime() != null && !properties.getEndTime().isEmpty() && res.getEndTime().equals(FormatUtil.getTimeHHmm(properties.getEndTime()))){
                return res;
            }
        }
        return res;
    }

    public StockResponse getStockResponse(String stock, Properties properties, List<Candle> candles, List<Candle> ystCandles) {

        long previousVolume = 0;
        List<Double> rsiList = new ArrayList<>();
        if(candles.size() < 2){
            log.info("Stock {} - No of candles found: {}",stock, candles.size());
            return null;
        }
        Candle firstCandle = candles.get(0);
        rsiList.add(firstCandle.getClose());
        String recentTime = stockDataManager.getRecentTime(stock, FormatUtil.getCurDate(properties));
        LocalTime recentTimeStamp = null;
        if (recentTime != null) {
            recentTimeStamp = LocalTime.parse(recentTime);
        }

        Map<Long, Double> map = new HashMap<>();
        double prevChgeInPer = 0.0;
        StockResponse res = null;
        Candle prevCandle = firstCandle;
        for (int i = 1; i <= candles.size() - 1; i++) {
            Candle curCandle = candles.get(i);
            rsiList.add(curCandle.getClose());
            if (rsiList.size() > 14) {
                rsiList.remove(0); // Remove the oldest element
            }
            LocalTime localTime = curCandle.getStartTime();
            double ltpChange = curCandle.getClose() - prevCandle.getClose();
            ltpChange = Double.parseDouble(String.format("%.2f", ltpChange));
            long oiChange = curCandle.getOpenInterest() - prevCandle.getOpenInterest();
            String oiInterpretation = (oiChange > 0)
                    ? (ltpChange > 0 ? "LBU" : "SBU")
                    : (ltpChange > 0 ? "SC" : "LU");
            double val1 = ((curCandle.getHigh() - curCandle.getLow()) / curCandle.getLow()) * 100;
            double firstCandleChgeInPer = (((firstCandle.getClose()- firstCandle.getOpen() ) / firstCandle.getOpen() ) * 100);
            double highToOpenChge = (((curCandle.getHigh()- curCandle.getOpen() ) / curCandle.getOpen() ) * 100);
            double lowToCloseChge = (((curCandle.getClose()- curCandle.getLow() ) / curCandle.getLow() ) * 100);

//            double value = rsiCalculator.calculateRSI(rsiList, 14);
            res = new StockResponse();
            res.setStock(stock);
            res.setOiInterpretation(oiInterpretation);
//            res.setRsi(value);
            res.setCurCandle(curCandle);
            res.setFirstCandle(firstCandle);
            if(!"OC".equals(properties.getType()) && curCandle.isHighVolume()) {
                if (properties.isFetchAll() || recentTimeStamp == null || localTime.isAfter(recentTimeStamp)) {
                    //&& firstCandleChgeInPer <0 && firstCandleChgeInPer>-1.1
                    if (curCandle.getClose() < firstCandle.getOpen() && lowToCloseChge < 0.4 && curCandle.getHigh() > firstCandle.getLow() &&(oiInterpretation.equals("SBU"))) {
//                        res.setStockType("N");
//                        return res;
                    }
                    // && firstCandleChgeInPer >0  && firstCandleChge`InPer <1.1
                    if (commonValidation.isPositive(candles,firstCandle, curCandle, oiInterpretation)) {
                        res.setStockType("P");
                        Candle c1 = commonValidation.validateYstStocks(stock, properties,ystCandles, curCandle);
                        if(c1 != null && res.getValidCandle() == null){
//                            log.info("Stock {} - Yst stock found: {}",stock,c1);
                        }
                        if(c1==null && res.getValidCandle() == null && curCandle.getEndTime().isBefore(LocalTime.of(10,30))) {
                            res.setValidCandle(curCandle);
                            return res;
                        }
                    }
                }
            }
            prevCandle = curCandle;
        }
        return res;
    }

}
