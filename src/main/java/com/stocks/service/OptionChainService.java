package com.stocks.service;

import com.stocks.dto.*;
import com.stocks.dto.Properties;
import com.stocks.utils.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OptionChainService {

    private static final Logger log = LoggerFactory.getLogger(OptionChainService.class);
    @Autowired
    IOPulseService ioPulseService;
    @Autowired
    StockDataManager stockDataManager;
    @Autowired
    CalculateOptionChain calculateOptionChain;
    @Autowired
    ProcessCandleSticks processCandleSticks;
    @Autowired
    private CommonValidation commonValidation;

    public List<StockResponse> getOptionChain(Properties properties) {
        List<StockResponse> data = new ArrayList<>();
        try {
            properties.setType("OC");
            List<StockData> stocks =  new ArrayList<>();
            stocks =       stockDataManager.getStocksByDate(FormatUtil.getCurDate(properties));
//            stocks.add(new StockData("HAL", "P"));
//            stocks.add(new StockData("PETRONET", "P"));
//            stocks.add(new StockData("CUMMINSIND", "P"));

            LocalTime startTime = null;
            if(properties.getStartTime() != null){
                 startTime = FormatUtil.getTime(properties.getStartTime(), 0);
            } else {
                startTime = LocalTime.now();
            }
//            List<String> stocks = Arrays.asList(properties.getStockName().split(","));
//            for (StockData stock : stocks) {
//               List<String> val = calculateOptionChain.processStock(stock.getStock(), stock.getType(), FormatUtil.formatTimeHHmmss(startTime.plusMinutes(-3)), properties);
//                if(!val.isEmpty()) {
//                    StockResponse res =  processCandleSticks.processApiResponse(properties, stock.getStock());
//                    res.setStockType(stock.getType());
//                    res.setOptionChain(val.get(0));
//                    res.setCurSt(val.get(1));
//                    res.setPutSt(val.get(2));
//                    res.setCallSt(val.get(4));
//                    data.add(res);
//               }
//            }
            LocalTime finalStartTime = startTime;
            data = stocks.stream()
                    .collect(Collectors.toMap(StockData::getStock, stock -> stock, (existing, replacement) -> existing)) // Ensure unique stocks by key
                    .values()
//                    .stream()
                    .parallelStream()
                    .map(stock -> {
                        List<String> val = calculateOptionChain.processStock(stock.getStock(), stock.getType(), FormatUtil.formatTimeHHmmss(finalStartTime.plusMinutes(-3)), properties);
                        if (!val.isEmpty()) {
                            List<Candle> candles = commonValidation.getCandles(properties, stock.getStock());
                            List<Candle> ystCandles = new ArrayList<>();
                            StockResponse res = processCandleSticks.getStockResponse( stock.getStock(),properties, candles, ystCandles);
                            res.setStockType(stock.getType());
                            res.setOptionChain(val.get(0));
                            res.setCurSt(val.get(1));
                            res.setPutSt(val.get(2));
                            res.setCallSt(val.get(4));
                            return res;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull) // Filter out null results
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error in getOptionChain: ", e);
        }

        return data;
    }

//    private void processStock(String stock, String type, String time, Properties properties) {
//        LocalTime endTime = FormatUtil.getTime("15:00:00", 0);
//        if (!"P".equals(type)) {
//            return;
//        }
//        LocalTime startTime = FormatUtil.getTime(time, 0);
////        while (startTime.isBefore(endTime)) {
////                    Thread.sleep(1000);
//        OptionChainResponse response = ioPulseService.getOptionChain(properties, stock, startTime);
//        if (response != null && response.getData() != null) {
//            UnderLyingAssetData underLyingAssetData = response.getData().getUnderLyingAssetData();
//            List<OptionChainData> list = response.getData().getData();
//            TreeMap<Integer, List<OptionChainData>> groupedData = list.stream()
//                    .collect(Collectors.groupingBy(
//                            OptionChainData::getInStrikePrice,
//                            TreeMap::new, // Use TreeMap to maintain sorted order
//                            Collectors.toList()
//                    ));
//            Integer focusKey = groupedData.floorKey((int) underLyingAssetData.getInLtp());

    /// /                    log.info("Start time: " + startTime + " Focus key: " + focusKey + " LTP: " + underLyingAssetData.getInLtp() + " group Keys " + String.join(",", groupedData.keySet().stream().map(String::valueOf).collect(Collectors.toList())));
    ///
    /// @return
//            int range = 5; // Number of records above and below
//            List<Integer> keys = new ArrayList<>(groupedData.keySet());
//            int focusIndex = keys.indexOf(focusKey);
//            List<OptionChainData> rowUp2 = null;
//            List<OptionChainData> rowDown2 = null;
//            List<OptionChainData> rowUp1 = null;
//            List<OptionChainData> rowDown1 = null;
//            List<OptionChainData> rowCur = null;
//            if (focusIndex != -1) {
//                if (focusIndex - range >= 0) {
//                    rowUp2 = groupedData.get(keys.get(focusIndex - range));
//                }
//                if (focusIndex + range < keys.size()) {
//                    rowDown2 = groupedData.get(keys.get(focusIndex + range));
//                }
//                if (focusIndex - range + 1 >= 0) {
//                    rowUp1 = groupedData.get(keys.get(focusIndex - range + 1));
//                }
//                if (focusIndex + range - 1 < keys.size()) {
//                    rowDown1 = groupedData.get(keys.get(focusIndex + range - 1));
//                }
//                rowCur = groupedData.get(keys.get(focusIndex));
//            }
//            OptionChainData rowUp2Call = null;
//            OptionChainData rowUp2Put = null;
//            if(rowUp2!= null){
//                if (rowUp2.size() >1){
//                    if("CE".equals(rowUp2.get(0).getStOptionsType())){
//                        rowUp2Call = rowUp2.get(0);
//                        rowUp2Put = rowUp2.get(1);
//                    } else {
//                        rowUp2Call = rowUp2.get(1);
//                        rowUp2Put = rowUp2.get(0);
//                    }
//                } else if(rowUp2.size() == 1){
//                    if("CE".equals(rowUp2.get(0).getStOptionsType())){
//                        rowUp2Call = rowUp2.get(0);
//                    } else {
//                        rowUp2Put = rowUp2.get(0);
//                    }
//                }
//            }
//            int maxPEVolume = 0;
//            int maxCEVolume = 0;
//            for (Map.Entry<Integer, List<OptionChainData>> entry : groupedData.entrySet()) {
//                for (OptionChainData data : entry.getValue()) {
//                    if ("PE".equals(data.getStOptionsType())) {
//                        maxPEVolume = Math.max(maxPEVolume, data.getInTradedVolume());
//                    } else if ("CE".equals(data.getStOptionsType())) {
//                        maxCEVolume = Math.max(maxCEVolume, data.getInTradedVolume());
//                    }
//                }
//            }
//            log.info("Highest PE Volume: " + maxPEVolume);
//            log.info("Highest CE Volume: " + maxCEVolume);
//        }
//    }

    boolean isValidStock(OptionChainData data1, OptionChainData data2, OptionChainData data3, int maxVolume) {
        return (data1 != null && data1.getInTradedVolume() == maxVolume) ||
                (data2 != null && data2.getInTradedVolume() == maxVolume) ||
                (data3 != null && data3.getInTradedVolume() == maxVolume);
    }

    int getVolume(OptionChainData data) {
        return data != null ? data.getInTradedVolume() : 0;
    }

    private List<OptionChainData> getRow(TreeMap<Integer, List<OptionChainData>> groupedData, List<Integer> keys, int index) {
        return (index >= 0 && index < keys.size()) ? groupedData.get(keys.get(index)) : null;
    }

    private OptionChainData getOptionByType(List<OptionChainData> row, String type) {
        return row.stream()
                .filter(data -> type.equals(data.getStOptionsType()))
                .findFirst()
                .orElse(null);
    }

//    public String processOptionChain(StockResponse res, Properties properties) {
//       List<String> details = processStock(res.getStock(), res.getStockType(), FormatUtil.formatTimeHHmmss(res.getStartTime()), properties);
//       return details.get(0);
//    }

    public String calculateExitTime(StockResponse stock, Properties properties) {
        String exitTime = null;
        LocalTime startTime = stock.getEndTime();
        while (startTime.isBefore(LocalTime.of(15,5))) {
            List<String> val = calculateOptionChain.processStock(stock.getStock(), stock.getStockType(), FormatUtil.formatTimeHHmmss(startTime), properties);
            if(!val.isEmpty()) {
                if (stock.getStockType().equals("P")) {
                    if ("-ve".equals(val.get(0))) {
                        exitTime = FormatUtil.formatTimeHHmm(startTime.plusMinutes(properties.getInterval()));
                        break;
                    }
                } else {
                    if ("+ve".equals(val.get(0))) {
                        exitTime = FormatUtil.formatTimeHHmm(startTime.plusMinutes(properties.getInterval()));
                        break;
                    }
                }
            }
            startTime = startTime.plusMinutes(properties.getInterval());
        }
        log.info("Exit time for stock " + stock.getStock() + " is " + exitTime);
        return exitTime;
    }
}
