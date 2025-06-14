package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.Properties;
import com.stocks.dto.StockResponse;
import com.stocks.utils.FormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class RangeBreakoutStrategy {

    @Autowired
    private ProcessCandleSticks processCandleSticks;
    @Autowired
    private RSICalculator rsiCalculator;
    @Autowired
    private VWAPStrategy vwapStrategy;
    @Autowired
    private CalculateOptionChain calculateOptionChain;
    @Autowired
    private CommonValidation commonValidation;

    public void breakOutStocks(Properties prop) {
        // Path to the file containing the stock list
        String filePath = "src/main/resources/stocksList.txt";

        // Read all lines from the file into a List
        List<String> stockList = Collections.emptyList();
        if (prop.getStockName() != null && !prop.getStockName().isEmpty()) {
            stockList = Arrays.asList(prop.getStockName().split(","));
        } else {
            try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
                stockList = lines.collect(Collectors.toList());
            } catch (IOException e) {
                log.error("Error reading file: " + e.getMessage());
            }
        }

        for (String stock : stockList) {
            Properties properties = new Properties();
            properties.setStockName(prop.getStockName());
            properties.setInterval(prop.getInterval());
            properties.setStockDate(FormatUtil.addDays(prop.getStockDate(), -1));
            List<Candle> ystCandles = commonValidation.getCandles(properties, stock);
            if(ystCandles.get(1).getHigh() < 100){
                continue;
            }
            List<Candle> candles = commonValidation.getCandles(prop, stock);
            List<Candle> newCandles = new ArrayList<>(ystCandles);
            List<Candle> todayCandles = new ArrayList<>();
            StockResponse res = new StockResponse();
            for (Candle candle : candles) {
                todayCandles.add(candle);
                newCandles.add(candle);

                if (todayCandles.size() > 1) {
//                StockResponse res = processCandleSticks.getStockResponse(stock, prop, todayCandles);
                    detectBreakout(newCandles, todayCandles, stock, res);
                }
            }
        }
    }

    public void detectBreakout(List<Candle> candles, List<Candle> todayCandles, String stock, StockResponse res) {
        int lookback = 14;
        if (candles.size() <= lookback) {
//            System.out.println("Not enough data.");
            return;
        }

        // Get last 10 candles for consolidation range
        List<Candle> recent = candles.subList(candles.size() - lookback - 1, candles.size() - 1);

        double rangeHigh = recent.stream().mapToDouble(Candle::getHigh).max().orElse(0);
        double rangeLow = recent.stream().mapToDouble(Candle::getLow).min().orElse(0);
        double range = rangeHigh - rangeLow;
        Candle lastCandle = candles.get(candles.size() - 1);
        double priceNow = lastCandle.getClose();
        // Check breakout on latest candle
//        log.info("{} OI {} - {} Entry: {} Stop-loss: {} at {} with volume {}", stock, lastCandle.getOiInt(),lastCandle.getOiChange(), String.format("%.2f", rangeHigh + 0.002 * priceNow), rangeLow, lastCandle.getEndTime(), lastCandle.getVolume());
        if (res.getStock() != null) {
            vwapStrategy.checkExitSignal(res, candles);
        }
        // Threshold: tight range < 1.5% of current price
        if ((range / priceNow) > 0.015) {
//            System.out.println("No consolidation detected.");
//            return;
        }

//        double avgVol = recent.stream().mapToDouble(Candle::getVolume).average().orElse(0);
        double todayAvgVol = todayCandles.stream().mapToDouble(Candle::getVolume).average().orElse(0);
        Candle firstCandle = todayCandles.get(0);
        List<Double> closeValues = candles.stream()
                .map(Candle::getClose)
                .collect(Collectors.toList());
        double threshold = 1.5; // Set your preferred threshold
        if (!"C".equals(res.getStockType())) {
            double topWick = lastCandle.getHigh() - Math.max(lastCandle.getOpen(), lastCandle.getClose());
            double bodySize = Math.abs(lastCandle.getOpen() - lastCandle.getClose());
            if (topWick < bodySize && priceNow > firstCandle.getHigh() && isPositive(candles) && lastCandle.getEndTime().isBefore(LocalTime.of(11, 00)) && lastCandle.getOpen() < lastCandle.getClose() && "LBU".equals(lastCandle.getOiInt())) {
                double value = vwapStrategy.calculateRSI(closeValues, 14);
                double atr = vwapStrategy.calculateATR(candles, 14);
                String earlierOi = "";
                Candle validatedCandle = null;

                if (earlierOi.isEmpty()) {
                    if (res.getStock() == null) {
                        res.setStock(stock);
                        res.setStopLoss(rangeLow);
                        res.setCurrentPrice(priceNow);
                        res.setStockType("C");
                    }
                    log.info("Bullish Breakout Detected! {} - RSI {} OI {} OI% {} - TOI% {} with volume {} is higher {} Open {} close {} Stop-loss: {} at {}", stock, String.format("%.2f", value), lastCandle.getOiInt(), lastCandle.getCurOiPer(),String.format("%.2f", lastCandle.getTotalOiPer()), lastCandle.getVolume(), lastCandle.isHighVolume(), lastCandle.getOpen(), lastCandle.getClose(), rangeLow, lastCandle.getEndTime());
                }
            } else if (lastCandle.getClose() < rangeLow && "SBU".equals(lastCandle.getOiInt())) {
//            log.info("Bearish Breakdown Detected! {} OI {} - {} Entry: {} Stop-loss: {} at {}", stock, lastCandle.getOiInt(), String.format("%.2f", oiPert), String.format("%.2f", rangeLow - 0.002 * priceNow), rangeHigh, lastCandle.getEndTime());
            } else {
//            System.out.println("No breakout yet.");
            }
        }

    }

    private boolean isPositive(List<Candle> candles) {
        Candle cur = candles.get(candles.size() - 1);
        Candle prev = candles.get(candles.size() - 2);
        return "LBU".equals(cur.getOiInt()) || "LBU".equals(prev.getOiInt());
    }
}

