package com.stocks.service;

import com.stocks.dto.FutureAnalysis;
import com.stocks.dto.HistoricalQuote;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrendLineService {


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
}
