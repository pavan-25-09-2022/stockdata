package com.stocks.service;

import com.stocks.dto.FutureAnalysis;
import com.stocks.dto.HistoricalQuote;
import com.stocks.dto.Properties;
import com.stocks.utils.FormatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class TrendLineService {

    @Autowired
    private CalculateOptionChain calculateOptionChain;


    public Map<String, List<String>> findTrendLines(Map<String, FutureAnalysis> stocksFutureAnalysis, boolean isPositive) {

        List<Map.Entry<String, FutureAnalysis>> entries = new ArrayList<>(stocksFutureAnalysis.entrySet());

        Map<String, List<String>> stockTrendLines = new LinkedHashMap<>();
        List<String> futureTrendLines = new ArrayList<>();
        List<String> spotTrenLines = new ArrayList<>();
        if (isPositive) {
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, FutureAnalysis> stringFutureAnalysisEntry = entries.get(i);
                FutureAnalysis futureAnalysis = stringFutureAnalysisEntry.getValue();
                HistoricalQuote historicalQuote = futureAnalysis.getHistoricalQuote();
                boolean isTrendLine = false;
                if (futureAnalysis.getInterpretation().equals("LBU")) {
                    StringBuilder rejectedAt = new StringBuilder( "rejected at :");
                    for (int j = i + 1; j < entries.size() - 1; j++) {

                        Map.Entry<String, FutureAnalysis> stringFutureAnalysisEntry1 = entries.get(j);
                        FutureAnalysis compareWithFutureAnalysis = stringFutureAnalysisEntry1.getValue();
                        if (compareWithFutureAnalysis.getLow() <= futureAnalysis.getLow() && compareWithFutureAnalysis.getClose() > futureAnalysis.getLow()
                                && futureAnalysis.getStrength() > compareWithFutureAnalysis.getStrength()) {
                            //futureTrendLines.add(futureAnalysis.getLow() + " at " +futureAnalysis.getDuration() +" rejected at " +compareWithFutureAnalysis.getDuration());
                            rejectedAt.append(compareWithFutureAnalysis.getDuration()).append("  ");
                            isTrendLine = true;
                        }
                        if (compareWithFutureAnalysis.getClose() < futureAnalysis.getLow()) {
                            isTrendLine = false;
                            break;
                        }
                    }

                    if (isTrendLine) {
                        //System.out.println("future time " + stringFutureAnalysisEntry.getKey());
                        futureTrendLines.add(futureAnalysis.getLow() + " at " +futureAnalysis.getDuration() + " " + rejectedAt.toString());
                    }

                    isTrendLine = false;

                    if (historicalQuote != null) {
                        StringBuilder quoteRejectedAt = new StringBuilder( "rejected at :");
                        for (int j = i + 1; j < entries.size() - 1; j++) {
                            Map.Entry<String, FutureAnalysis> stringFutureAnalysisEntry2 = entries.get(j);
                            FutureAnalysis compareWithFutureAnalysis = stringFutureAnalysisEntry2.getValue();
                            HistoricalQuote compareWithHistoricalQuote = compareWithFutureAnalysis.getHistoricalQuote();
                            if (compareWithHistoricalQuote == null) {
                                continue;
                            }
                            if (compareWithHistoricalQuote.getLow().compareTo(historicalQuote.getLow()) <= 0
                                    && compareWithHistoricalQuote.getClose().compareTo(historicalQuote.getLow()) > 0
                                    //&& futureAnalysis.getStrength() > compareWithFutureAnalysis.getStrength()) {
                            ){
                                //spotTrenLines.add(historicalQuote.getLow()+ " at "+futureAnalysis.getDuration() +" rejected at " +compareWithHistoricalQuote.getDate().getTime());
                                isTrendLine = true;
                                quoteRejectedAt.append(compareWithFutureAnalysis.getDuration()).append("  ");
                            }
                            if (compareWithHistoricalQuote.getClose().compareTo(historicalQuote.getLow()) < 0) {
                                isTrendLine = false;
                                break;
                            }
                        }
                       if (isTrendLine) {
                            spotTrenLines.add(historicalQuote.getLow()+ " at "+futureAnalysis.getDuration() + " " + quoteRejectedAt.toString());
                        }

                    }

                }
            }
        } else {
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, FutureAnalysis> stringFutureAnalysisEntry = entries.get(i);
                FutureAnalysis futureAnalysis = stringFutureAnalysisEntry.getValue();
                HistoricalQuote historicalQuote = futureAnalysis.getHistoricalQuote();
                boolean isTrendLine = false;
                if (futureAnalysis.getInterpretation().equals("SBU")) {
                    StringBuilder rejectedAt = new StringBuilder( "rejected at :");
                    for (int j = i + 1; j < entries.size() - 1; j++) {

                        Map.Entry<String, FutureAnalysis> stringFutureAnalysisEntry1 = entries.get(j);
                        FutureAnalysis compareWithFutureAnalysis = stringFutureAnalysisEntry1.getValue();
                        if (compareWithFutureAnalysis.getHigh() >= futureAnalysis.getHigh()
                                && compareWithFutureAnalysis.getClose() < futureAnalysis.getHigh()
                                && compareWithFutureAnalysis.getStrength() < futureAnalysis.getStrength()) {
                            isTrendLine = true;
                            rejectedAt.append(compareWithFutureAnalysis.getDuration()).append("  ");
                            //futureTrendLines.add(futureAnalysis.getHigh()+ " at "+futureAnalysis.getDuration()+" rejected at " +compareWithFutureAnalysis.getDuration());
                        }
                        if (compareWithFutureAnalysis.getClose() > futureAnalysis.getHigh()) {
                            isTrendLine = false;
                            break;
                        }
                    }

                    if (isTrendLine) {
                        futureTrendLines.add(futureAnalysis.getHigh()+ " at "+futureAnalysis.getDuration() + " " + rejectedAt.toString());
                    }

                    isTrendLine = false;

                    if(historicalQuote == null){
                        continue;
                    }
                    StringBuilder quoteRejectedAt = new StringBuilder( "rejected at :");
                    for (int j = i + 1; j < entries.size() - 1; j++) {
                        Map.Entry<String, FutureAnalysis> stringFutureAnalysisEntry2 = entries.get(j);
                        FutureAnalysis compareWithFutureAnalysis = stringFutureAnalysisEntry2.getValue();
                        HistoricalQuote compareWithHistoricalQuote = compareWithFutureAnalysis.getHistoricalQuote();
                        if (compareWithHistoricalQuote.getHigh().compareTo(historicalQuote.getHigh()) > 0 && compareWithHistoricalQuote.getClose().compareTo(historicalQuote.getHigh()) < 0) {
                            isTrendLine = true;
                            quoteRejectedAt.append(compareWithFutureAnalysis.getDuration()).append("  ");
                            //spotTrenLines.add(historicalQuote.getHigh()+ " at "+futureAnalysis.getDuration()+" rejected at " +compareWithHistoricalQuote.getDate().getTime());
                        }
                        if (compareWithHistoricalQuote.getClose().compareTo(historicalQuote.getHigh()) > 0) {
                            isTrendLine = false;
                            break;
                        }
                    }
                    if (isTrendLine) {
                        spotTrenLines.add(historicalQuote.getHigh()+ " at "+futureAnalysis.getDuration() +   " " + quoteRejectedAt.toString());
                    }

                }
            }
        }
        if (!futureTrendLines.isEmpty()) {
            stockTrendLines.put("future", futureTrendLines);
        }
        if (!stockTrendLines.isEmpty()) {
            stockTrendLines.put("spot", spotTrenLines);
        }
        System.gc();
        return stockTrendLines;
    }

    public void findTrend(Map<String, FutureAnalysis> stocksFutureAnalysis, String stock, Properties properties, StringBuilder message) {

        List<Map.Entry<String, FutureAnalysis>> entries = new ArrayList<>(stocksFutureAnalysis.entrySet());

        for (int i = 1; i < entries.size(); i++) {
            Map.Entry<String, FutureAnalysis> stringFutureAnalysisEntry = entries.get(i);
            FutureAnalysis futureAnalysis = stringFutureAnalysisEntry.getValue();
            if (futureAnalysis.getOiPercentageChange() > 1 ) {
                boolean isShortCover =false;
                boolean isLongUnwinding = false;
                int min = Math.min(i + 5, entries.size() - 1);
                for (int j = i + 1; j < min; j++) {
                    System.out.println("Comparing " + futureAnalysis.getDuration() + " with " + entries.get(j).getKey());
                    Map.Entry<String, FutureAnalysis> stringFutureAnalysisEntry1 = entries.get(j);
                    FutureAnalysis compareWithFutureAnalysis = stringFutureAnalysisEntry1.getValue();
                    if (compareWithFutureAnalysis.getInterpretation().equals("SC")
                            //&& compareWithFutureAnalysis.getVolume() > futureAnalysis.getVolume()
                            && compareWithFutureAnalysis.getClose() > futureAnalysis.getHigh()) {
                        String startTime = futureAnalysis.getDuration().split("-")[0]+":00";
                        String endTime = compareWithFutureAnalysis.getDuration().split("-")[1]+":00";
                        properties.setStartTime(startTime);
                        properties.setEndTime(endTime);
                         message.append(stock +" positive on " +properties.getStockDate()+ " from " + startTime + " to " + endTime);
                        checkOptionChain(stock, properties, message);
                        isShortCover = true;
                    }
                    if (compareWithFutureAnalysis.getInterpretation().equals("LU")
                            //&& compareWithFutureAnalysis.getVolume() > futureAnalysis.getVolume()
                            && compareWithFutureAnalysis.getClose() < futureAnalysis.getLow()) {
                        String startTime = futureAnalysis.getDuration().split("-")[0]+":00";
                        String endTime = compareWithFutureAnalysis.getDuration().split("-")[1]+":00";
                        properties.setStartTime(startTime);
                        properties.setEndTime(endTime);
                        message.append(stock+" negative on " +properties.getStockDate()+ " from " + startTime + " to " + endTime);
                        checkOptionChain(stock, properties, message);
                        isLongUnwinding = true;
                    }

                    if (isShortCover || isLongUnwinding) {
                        break;
                    }
                }
            }
        }
    }

    public void checkOptionChain(String stock, Properties properties, StringBuilder message) {
        for (int i = 0; i < 5; i++) {
            if (calculateOptionChain.changeInOI(stock, properties.getStartTime(), properties, message.toString().contains("positive"), null)) {
                if( i > 0) {
                    System.out.println("Option chain found for " + stock + " at " + properties.getStartTime());
                    message.append("  And Option short covered from ").append(properties.getStartTime()).append(" to  ").append(properties.getEndTime());
                }
            }
            properties.setEndTime(FormatUtil.getTime(properties.getEndTime(), properties.getInterval()).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }
}
