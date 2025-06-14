package com.stocks.service;

import com.stocks.dto.OptionChainData;
import com.stocks.dto.OptionChainResponse;
import com.stocks.dto.Properties;
import com.stocks.dto.UnderLyingAssetData;
import com.stocks.utils.FormatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CalculateOptionChain {

    @Autowired
    private IOPulseService ioPulseService;

    public List<String> processStock(String stock, String type, String time, Properties properties) {
        LocalTime startTime = FormatUtil.getTime(time, 0);
        List<String> details = new ArrayList<>();
        OptionChainResponse response = ioPulseService.getOptionChain(properties, stock, startTime);
        if (response == null || response.getData() == null) {
            return details;
        }

        UnderLyingAssetData underLyingAssetData = response.getData().getUnderLyingAssetData();
        List<OptionChainData> list = response.getData().getData();

        TreeMap<Integer, List<OptionChainData>> groupedData = list.stream()
                .collect(Collectors.groupingBy(
                        OptionChainData::getInStrikePrice,
                        TreeMap::new,
                        Collectors.toList()
                ));

        Integer focusKey = groupedData.floorKey((int) underLyingAssetData.getInLtp());
        if (focusKey == null) {
            return details;
        }

        List<Integer> keys = new ArrayList<>(groupedData.keySet());
        int focusIndex = keys.indexOf(focusKey);
        if (focusIndex == -1) {
            return details;
        }

        // Extract rows based on range
//        int range = 5;
//        List<OptionChainData> rowUp2 = getRow(groupedData, keys, focusIndex - range);
//        List<OptionChainData> rowDown2 = getRow(groupedData, keys, focusIndex + range);
//        List<OptionChainData> rowUp1 = getRow(groupedData, keys, focusIndex - range + 1);
//        List<OptionChainData> rowDown1 = getRow(groupedData, keys, focusIndex + range - 1);
//        List<OptionChainData> rowCur = getRow(groupedData, keys, focusIndex);
//
//        // Process rowUp2 for Call and Put
//        OptionChainData rowUp2Call = null;
//        OptionChainData rowUp2Put = null;
//        if (rowUp2 != null) {
//            rowUp2Call = getOptionByType(rowUp2, "CE");
//            rowUp2Put = getOptionByType(rowUp2, "PE");
//        }
//        // Process rowUp2 for Call and Put
//        OptionChainData rowUp1Call = null;
//        OptionChainData rowUp1Put = null;
//        if (rowUp1 != null) {
//            rowUp1Call = getOptionByType(rowUp1, "CE");
//            rowUp1Put = getOptionByType(rowUp1, "PE");
//        }
//        // Process cur row for Call and Put
//        OptionChainData curCall = null;
//        OptionChainData curPut = null;
//        if (rowCur != null) {
//            curCall = getOptionByType(rowCur, "CE");
//            curPut = getOptionByType(rowCur, "PE");
//        }
//        // Process rowDown1 for Call and Put
//        OptionChainData rowDown1Call = null;
//        OptionChainData rowDown1Put = null;
//        if (rowDown1 != null) {
//            rowDown1Call = getOptionByType(rowDown1, "CE");
//            rowDown1Put = getOptionByType(rowDown1, "PE");
//        }
//        // Process rowDown2 for Call and Put
//        OptionChainData rowDown2Call = null;
//        OptionChainData rowDown2Put = null;
//        if (rowDown2 != null) {
//            rowDown2Call = getOptionByType(rowDown2, "CE");
//            rowDown2Put = getOptionByType(rowDown2, "PE");
//        }

        // Calculate max volumes using streams
        int maxPEVolume = groupedData.values().stream()
                .flatMap(Collection::stream)
                .filter(data -> "PE".equals(data.getStOptionsType()))
                .mapToInt(OptionChainData::getInTradedVolume)
                .max()
                .orElse(0);

        int maxCEVolume = groupedData.values().stream()
                .flatMap(Collection::stream)
                .filter(data -> "CE".equals(data.getStOptionsType()))
                .mapToInt(OptionChainData::getInTradedVolume)
                .max()
                .orElse(0);

        boolean isMaxCEVolumeBelowFocusKey = false;
        boolean isMaxPEVolumeAboveFocusKey = false;

        // Get all keys below and above the focusKey
        List<Integer> aboveRows = keys.subList(0, focusIndex+1);
        List<Integer> belowRows = keys.subList(focusIndex , keys.size());

//        log.info("below rows " + belowRows );
//        log.info("above rows " + aboveRows);

//        isMaxCEVolumeBelowFocusKey = belowRows.stream()
//                .map(groupedData::get)
//                .flatMap(Collection::stream)
//                .anyMatch(data -> "CE".equals(data.getStOptionsType()) && data.getInTradedVolume() == maxCEVolume)
//                ;
//
//        isMaxPEVolumeAboveFocusKey = aboveRows.stream()
//                .map(groupedData::get)
//                .flatMap(Collection::stream)
//                .anyMatch(data -> "PE".equals(data.getStOptionsType()) && data.getInTradedVolume() == maxPEVolume)
//                ;

        Map.Entry<Integer, Integer> maxCEVolumeEntry = groupedData.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .filter(data -> "CE".equals(data.getStOptionsType()))
                        .map(data -> new AbstractMap.SimpleEntry<>(entry.getKey(), data.getInTradedVolume())))
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .orElse(new AbstractMap.SimpleEntry<>(-1, 0)); // Default if no CE data is found

        Map.Entry<Integer, Integer> maxPEVolumeEntry = groupedData.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .filter(data -> "PE".equals(data.getStOptionsType()))
                        .map(data -> new AbstractMap.SimpleEntry<>(entry.getKey(), data.getInTradedVolume())))
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .orElse(new AbstractMap.SimpleEntry<>(-1, 0)); // Default if no PE data is found

        isMaxCEVolumeBelowFocusKey = belowRows.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .anyMatch(data -> "CE".equals(data.getStOptionsType()) && data.getInTradedVolume() == maxCEVolumeEntry.getValue());

        isMaxPEVolumeAboveFocusKey = aboveRows.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .anyMatch(data -> "PE".equals(data.getStOptionsType()) && data.getInTradedVolume() == maxPEVolumeEntry.getValue());


//        log.info("Is maxCEVolume below focus key (within 5): " + isMaxCEVolumeBelowFocusKey);
//        log.info("Is maxPEVolume above focus key (within 5): " + isMaxPEVolumeBelowFocusKey);
//        log.info("Stock: " + stock + " HPE " + maxPEVolume + " HCE " + maxCEVolume + " rowUo1cll " + getVolume(rowUp1Call) + " rowDown1Put " + getVolume(rowDown1Put) + " curCall " + getVolume(curCall) + " curPut " + getVolume(curPut) + " rowUp2Call " + getVolume(rowUp2Call) + " rowDown2Put " + getVolume(rowDown2Put));
//        int curPutVol = getVolume(curPut);
//        int curCallVol = getVolume(curCall);
//        int rowUp1PutVol = getVolume(rowUp1Put);
//        int rowUp2PutVol = getVolume(rowUp2Put);
//        int rowDown1CalVol = getVolume(rowDown1Put);
//        if("P".equals(type)) {
////            if (maxCEVolume > maxPEVolume && isValidStock(rowDown1Call, rowDown2Call, curCall, maxCEVolume))
//            if(!isMaxPEVolumeBelowFocusKey && isMaxCEVolumeBelowFocusKey)
//            {
//
////            if (maxPEVolume > curPutVol && maxPEVolume > rowUp1PutVol)
//                {
////                    log.info("Stock " + stock + " is valid");
//                    return true;
//                }
//            }
//        } else if("N".equals(type)){
////            if (maxPEVolume > maxCEVolume && isValidStock(rowDown1Put, rowDown2Put, curPut, maxPEVolume))
//            if(!isMaxCEVolumeBelowFocusKey && isMaxPEVolumeBelowFocusKey)
//            {
////            if (maxPEVolume > curPutVol && maxPEVolume > rowUp1PutVol)
//                {
////                    log.info("Stock " + stock + " is valid");
//                    return true;
//                }
//            }
//        }
        if(isMaxCEVolumeBelowFocusKey && isMaxPEVolumeAboveFocusKey){
//            log.info("Stock " + stock + " is positive");
            details.add( "+ve");
        } else {
//            log.info("Stock " + stock + " is negative");
            details.add("-ve");
        }
        details.add(String.valueOf(focusKey));
        details.add(String.valueOf(maxPEVolumeEntry.getKey()));
        details.add(String.valueOf(maxPEVolumeEntry.getValue()));
        details.add(String.valueOf(maxCEVolumeEntry.getKey()));
        details.add(String.valueOf(maxCEVolumeEntry.getValue()));

        return details;
    }


public boolean isValidStock(String stock, String time, Properties properties, boolean isPositive) {
        String time1 = properties.getStartTime() != null ? properties.getStartTime() : "09:15:00";
    LocalTime startTime = FormatUtil.getTime(time1, 0);
    OptionChainResponse response = ioPulseService.getOptionChain(properties, stock, startTime);
    if (response == null || response.getData() == null) {
        return false;
    }

    UnderLyingAssetData underLyingAssetData = response.getData().getUnderLyingAssetData();
    List<OptionChainData> list = response.getData().getData();

    TreeMap<Integer, List<OptionChainData>> groupedData = list.stream()
            .collect(Collectors.groupingBy(
                    OptionChainData::getInStrikePrice,
                    TreeMap::new,
                    Collectors.toList()
            ));

    Integer focusKey = groupedData.floorKey((int) underLyingAssetData.getInLtp());
    if (focusKey == null) {
        return false;
    }

    List<Integer> keys = new ArrayList<>(groupedData.keySet());
    int focusIndex = keys.indexOf(focusKey);
    if (focusIndex == -1) {
        return false;
    }

    boolean isMaxCEVolumeBelowFocusKey = false;
    boolean isMaxPEVolumeAboveFocusKey = false;

    // Get all keys below and above the focusKey
    List<Integer> aboveRows = keys.subList(0, focusIndex+1);
    List<Integer> belowRows = keys.subList(focusIndex , keys.size());

    Map.Entry<Integer, Integer> maxCEVolumeEntry = groupedData.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                    .filter(data -> "CE".equals(data.getStOptionsType()))
                    .map(data -> new AbstractMap.SimpleEntry<>(entry.getKey(), data.getInTradedVolume())))
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .orElse(new AbstractMap.SimpleEntry<>(-1, 0)); // Default if no CE data is found

    Map.Entry<Integer, Integer> maxPEVolumeEntry = groupedData.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                    .filter(data -> "PE".equals(data.getStOptionsType()))
                    .map(data -> new AbstractMap.SimpleEntry<>(entry.getKey(), data.getInTradedVolume())))
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .orElse(new AbstractMap.SimpleEntry<>(-1, 0)); // Default if no PE data is found

    Map.Entry<Integer, Integer> maxCEExistOIEntry = groupedData.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                    .filter(data -> "CE".equals(data.getStOptionsType()))
                    .map(data -> new AbstractMap.SimpleEntry<>(entry.getKey(), (data.getInNewOi() - data.getInOldOi()))))
            .min(Comparator.comparingInt(Map.Entry::getValue))
            .orElse(new AbstractMap.SimpleEntry<>(-1, 0));

    Map.Entry<Integer, Integer> maxPEExistOIEntry = groupedData.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                    .filter(data -> "PE".equals(data.getStOptionsType()))
                    .map(data -> new AbstractMap.SimpleEntry<>(entry.getKey(), (data.getInNewOi() - data.getInOldOi()))))
            .min(Comparator.comparingInt(Map.Entry::getValue))
            .orElse(new AbstractMap.SimpleEntry<>(-1, 0));

    isMaxCEVolumeBelowFocusKey = belowRows.stream()
            .map(groupedData::get)
            .flatMap(Collection::stream)
            .anyMatch(data -> "CE".equals(data.getStOptionsType()) && data.getInTradedVolume() == maxCEVolumeEntry.getValue());

    isMaxPEVolumeAboveFocusKey = aboveRows.stream()
            .map(groupedData::get)
            .flatMap(Collection::stream)
            .anyMatch(data -> "PE".equals(data.getStOptionsType()) && data.getInTradedVolume() == maxPEVolumeEntry.getValue());


    // Get 3 below (if available)
    int fromBelow = Math.max(0, focusIndex - 4);
    int toBelow = focusIndex; // exclusive
    List<Integer> below3 = keys.subList(fromBelow, toBelow);

    // Get 3 above (if available)
    int fromAbove = focusIndex + 1;
    int toAbove = Math.min(keys.size(), focusIndex + 5);
    List<Integer> above3 = keys.subList(fromAbove, toAbove);

    boolean isValidStock = false;

    if(isPositive) {

        // For CE: at least 2 of the 3 below have negative OI change
        long negativeCECount = below3.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "CE".equals(data.getStOptionsType()))
                .filter(data -> (data.getInNewOi() - data.getInOldOi()) < 0)
                .count();

        boolean atLeastTwoNegativeCEBelow = negativeCECount >= 2;

        // For PE: all 3 above have positive OI change
        boolean allPositivePE = below3.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "PE".equals(data.getStOptionsType()))
                .allMatch(data -> (data.getInNewOi() - data.getInOldOi()) > 0);

        // For CE: at least 2 of the 3 below have negative OI change
        long positivePECount = above3.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "PE".equals(data.getStOptionsType()))
                .filter(data -> (data.getInNewOi() - data.getInOldOi()) > 0)
                .count();

        boolean atLeastTwoPositivePEAbove = positivePECount >= 2;

        isValidStock = atLeastTwoNegativeCEBelow && allPositivePE && atLeastTwoPositivePEAbove && below3.contains(maxCEExistOIEntry.getKey());
    } else {

        // For CE: at least 2 of the 3 below have negative OI change
        long negativePECount = above3.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "PE".equals(data.getStOptionsType()))
                .filter(data -> (data.getInNewOi() - data.getInOldOi()) < 0)
                .count();

        boolean atLeastTwoNegativePEBelow = negativePECount >= 2;

        // For PE: all 3 above have positive OI change
        boolean allPositiveCE = above3.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "CE".equals(data.getStOptionsType()))
                .allMatch(data -> (data.getInNewOi() - data.getInOldOi()) > 0);

        // For CE: at least 2 of the 3 below have negative OI change
        long positivePECount = below3.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "CE".equals(data.getStOptionsType()))
                .filter(data -> (data.getInNewOi() - data.getInOldOi()) > 0)
                .count();

        boolean atLeastTwoPositiveCEBelow = positivePECount >= 2;

        isValidStock = atLeastTwoNegativePEBelow && allPositiveCE && atLeastTwoPositiveCEBelow && above3.contains(maxPEExistOIEntry.getKey());
    }

    return isMaxCEVolumeBelowFocusKey && isMaxPEVolumeAboveFocusKey &&  isValidStock;
}

    public boolean changeInOI(String stock, String time, Properties properties, boolean isPositive) {
        LocalTime startTime = FormatUtil.getTime(time, 0);
        OptionChainResponse response = ioPulseService.getOptionChain(properties, stock, startTime);
        if (response == null || response.getData() == null) {
            return false;
        }

        UnderLyingAssetData underLyingAssetData = response.getData().getUnderLyingAssetData();
        List<OptionChainData> list = response.getData().getData();

        TreeMap<Integer, List<OptionChainData>> groupedData = list.stream()
                .collect(Collectors.groupingBy(
                        OptionChainData::getInStrikePrice,
                        TreeMap::new,
                        Collectors.toList()
                ));

        Integer focusKey = groupedData.floorKey((int) underLyingAssetData.getInLtp());
        if (focusKey == null) {
            return false;
        }

        List<Integer> keys = new ArrayList<>(groupedData.keySet());
        int focusIndex = keys.indexOf(focusKey);
        if (focusIndex == -1) {
            return false;
        }

        int fromBelow = Math.max(0, focusIndex - 4);
        int toBelow = focusIndex; // exclusive
        List<Integer> belowStricks = keys.subList(fromBelow, toBelow);

        int fromAbove = focusIndex + 2;
        int toAbove = Math.min(keys.size(), focusIndex + 6);
        List<Integer> aboveStricks = keys.subList(fromAbove, toAbove);

        long totalPEOIAboveStricks = aboveStricks.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "PE".equals(data.getStOptionsType()))
                .mapToLong(OptionChainData::getOIChange)
                .sum();

        long totalCEOIAboveStricks = aboveStricks.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "CE".equals(data.getStOptionsType()))
                .mapToLong(OptionChainData::getOIChange)
                .sum();


        long totalCEOIBelowStricks = belowStricks.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "CE".equals(data.getStOptionsType()))
                .mapToLong(OptionChainData::getOIChange)
                .sum();
        long totalPEOIBelowStricks = belowStricks.stream()
                .map(groupedData::get)
                .flatMap(Collection::stream)
                .filter(data -> "PE".equals(data.getStOptionsType()))
                .mapToLong(OptionChainData::getOIChange)
                .sum();
        boolean isPEGreater = totalPEOIAboveStricks > totalCEOIAboveStricks;
        System.out.println("Total CE OI below stricks: " + totalCEOIAboveStricks + " Total PE OI above stricks: " + totalPEOIAboveStricks + " Trend " + (isPEGreater ? "Positive" : "Negative") + " at " +startTime);

        return isPositive ? totalCEOIBelowStricks < 0 && totalPEOIAboveStricks > 0 : totalCEOIBelowStricks > 0 && totalPEOIAboveStricks < 0;
    }
}
