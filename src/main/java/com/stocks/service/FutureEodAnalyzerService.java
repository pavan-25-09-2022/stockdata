package com.stocks.service;

import com.stocks.dto.FutureEodAnalyzer;
import com.stocks.dto.Properties;
import com.stocks.dto.StockEODResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FutureEodAnalyzerService {

    @Autowired
    private IOPulseService ioPulseService;

    @Autowired
    private MailService mailService;

    @Value("${calculateEodValue}")
    private int calculateEodValue;

    private static final Logger log = LoggerFactory.getLogger(FutureEodAnalyzerService.class);

    public String processEodResponse(Properties properties) {
        // Path to the file containing the stock list
        String filePath = "src/main/resources/stocksList.txt";

        // Read all lines from the file into a List
        List<String> stockList;
        if(properties.getStockName() != null){
            stockList = Arrays.asList(properties.getStockName().split(","));
        } else {
            try (java.util.stream.Stream<String> lines = Files.lines(Paths.get(filePath))) {
                stockList = lines.collect(Collectors.toList());
            } catch (IOException e) {
                log.error("Error reading file: " + e.getMessage());
                return "No Records";
            }
        }


        List<String> longunwindingFollowedByShortCovering = new ArrayList<>();
        longunwindingFollowedByShortCovering.add("Eod Analyser");
        longunwindingFollowedByShortCovering.add("\n");
        longunwindingFollowedByShortCovering.add("... Long unwinding followed by short covering.....");

        List<String> shortCoveringFollowedByLongBuildUp = new ArrayList<>();
        shortCoveringFollowedByLongBuildUp.add("Eod Analyser");
        shortCoveringFollowedByLongBuildUp.add("\n");
        shortCoveringFollowedByLongBuildUp.add("... Short Covering followed by Long Build Up.....");

        List<String> shortCoveringFollowedByLongUnwinding = new ArrayList<>();
        shortCoveringFollowedByLongUnwinding.add("Eod Analyser");
        shortCoveringFollowedByLongUnwinding.add("\n");
        shortCoveringFollowedByLongUnwinding.add("... Short Covering followed by LongUnWinding.....");

        List<String> longUnwindingFollowedByShortBuildUp = new ArrayList<>();
        longUnwindingFollowedByShortBuildUp.add("Eod Analyser");
        longUnwindingFollowedByShortBuildUp.add("\n");
        longUnwindingFollowedByShortBuildUp.add("... Long Unwinding followed by short Build Up.....");

        boolean isLongunwindingFollowedByShortCovering = false;
        boolean isShortCoveringFollowedByLongBuildUp = false;
        boolean isShortCoveringFollowedByLongUnwinding = false;
        boolean isLongUnwindingFollowedByShortBuildUp = false;

        for (String stock : stockList) {

            StockEODResponse eod = ioPulseService.getMonthlyData(stock);
            if (eod == null || eod.getData().size() < 3) {
                log.info("EOD data is insufficient for stock: {}", stock);
                continue;
            }

            // Calculate
            if (eod.getData().size() > calculateEodValue) {
                for (int i = 0; i < calculateEodValue; i++) {
                    calculateOiInterpretation(eod.getData().get(i), eod.getData().get(i + 1));
                    System.out.println("StockDetails" +eod.getData().get(i+1));
                    System.out.println("Next Day StockDetails"+eod.getData().get(i));
                }

                for (int i = calculateEodValue-1; i > 0; i--) {

                    FutureEodAnalyzer stockDetails = eod.getData().get(i);
                    FutureEodAnalyzer nextDayStockDetails = eod.getData().get(i - 1);

                    if (("LU".equals(stockDetails.getOiInterpretation()) && "SC".equals(nextDayStockDetails.getOiInterpretation())) ||
                            ("SC".equals(stockDetails.getOiInterpretation()) && "LBU".equals(nextDayStockDetails.getOiInterpretation()))) {

                        boolean highCondition = (nextDayStockDetails.getInDayHigh() > stockDetails.getInDayHigh());
                        boolean closeCondition = (nextDayStockDetails.getInClose() > stockDetails.getInDayHigh());
                        boolean strengthCondition = (nextDayStockDetails.getStrength() > stockDetails.getStrength());
                        boolean nextdayLow = stockDetails.getInDayLow() < nextDayStockDetails.getInDayLow();

                        if (highCondition && closeCondition && strengthCondition && nextdayLow) {

                            if ("LU".equals(stockDetails.getOiInterpretation())) {
                                longunwindingFollowedByShortCovering.add("\n");
                                longunwindingFollowedByShortCovering.add("..... .." + stock + " ......with volume " + (nextDayStockDetails.getInVolume() > stockDetails.getInVolume()));
                                isLongunwindingFollowedByShortCovering = true;
                            } else {
                                shortCoveringFollowedByLongBuildUp.add("\n");
                                shortCoveringFollowedByLongBuildUp.add("..... .." + stock + " ......with volume " + (nextDayStockDetails.getInVolume() > stockDetails.getInVolume()));
                                isShortCoveringFollowedByLongBuildUp = true;
                            }

                        }
                    } else if (("SC".equals(stockDetails.getOiInterpretation()) && "LU".equals(nextDayStockDetails.getOiInterpretation()))
                            || ("LU".equals(stockDetails.getOiInterpretation()) && "SB".equals(nextDayStockDetails.getOiInterpretation()))) {


                        boolean negativeLowCondition = (nextDayStockDetails.getInDayLow() < stockDetails.getInDayLow());
                        boolean negativeCloseCondition = (nextDayStockDetails.getInClose() < stockDetails.getInDayLow());
                        boolean negativeStrengthCondition = (nextDayStockDetails.getStrength() > stockDetails.getStrength());
                        boolean negativeHighCondition = (nextDayStockDetails.getInDayHigh() < stockDetails.getInDayHigh());

                        if (negativeLowCondition && negativeCloseCondition && negativeStrengthCondition && negativeHighCondition) {
                            if ("SC".equals(stockDetails.getOiInterpretation())) {
                                shortCoveringFollowedByLongUnwinding.add("\n");
                                shortCoveringFollowedByLongUnwinding.add("..... .." + stock + " ....... with volume " + (nextDayStockDetails.getInVolume() > stockDetails.getInVolume()));
                                isShortCoveringFollowedByLongUnwinding = true;
                            } else {
                                longUnwindingFollowedByShortBuildUp.add("\n");
                                longUnwindingFollowedByShortBuildUp.add("..... .." + stock + " ......with volume " + (nextDayStockDetails.getInVolume() > stockDetails.getInVolume()));
                                isLongUnwindingFollowedByShortBuildUp = true;
                            }
                        }

                    }

                }
            }
        }

        if (isLongUnwindingFollowedByShortBuildUp) {
            mailService.sendEmailList(longUnwindingFollowedByShortBuildUp, "Negative Stocks with LU followed SBU ");
        }

        if (isLongunwindingFollowedByShortCovering) {
            mailService.sendEmailList(longunwindingFollowedByShortCovering, "Positive Stocks with LU followed by SC");
        }

        if (isShortCoveringFollowedByLongBuildUp) {
            mailService.sendEmailList(shortCoveringFollowedByLongBuildUp, "Positive Stocks with SC followed by LBU");
        }

        if (isShortCoveringFollowedByLongUnwinding) {
            mailService.sendEmailList(shortCoveringFollowedByLongUnwinding, "Negative Stocks with SC followed by LU");
        }

        List<String> stocks = new ArrayList<>();
        stocks.addAll(shortCoveringFollowedByLongBuildUp);
        stocks.addAll(longunwindingFollowedByShortCovering);
        stocks.addAll(longUnwindingFollowedByShortBuildUp);
        stocks.addAll(shortCoveringFollowedByLongUnwinding);
        return stocks.toString();
    }

    private void calculateOiInterpretation(FutureEodAnalyzer current, FutureEodAnalyzer previous) {
        double ltpChange = Double.parseDouble(String.format("%.2f", current.getInClose() - previous.getInClose()));
        long oiChange = Long.parseLong(current.getInOi()) - Long.parseLong(previous.getInOi());
        String oiInterpretation = (oiChange > 0) ? (ltpChange > 0 ? "LBU" : "SBU") : (ltpChange > 0 ? "SC" : "LU");
        if(oiChange != 0) {
            DecimalFormat df = new DecimalFormat("#.###########");
            double strength = Math.abs(Double.parseDouble(df.format(ltpChange / oiChange)));
            current.setStrength(strength);
        }
        current.setLtpChange(ltpChange);
        current.setOiChange(oiChange);
        current.setOiInterpretation(oiInterpretation);

    }


}
