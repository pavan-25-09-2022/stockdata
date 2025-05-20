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
            properties.setStockDate(FormatUtil.getYesterdayDate(properties.getStockDate()));
            List<Candle> ystCandles = processCandleSticks.getCandles(properties, stock);
            List<Candle> candles = processCandleSticks.getCandles(prop, stock);
            List<Candle> newCandles = new ArrayList<>();
            int ystCount = ystCandles.size();
            if (ystCount >= 15) {
                newCandles.addAll(ystCandles.subList(ystCount - 15, ystCount));
            } else {
                newCandles.addAll(ystCandles);
            }
            for (Candle candle : candles) {
                newCandles.add(candle);
                StockResponse res = processCandleSticks.getStockResponse(stock, prop, newCandles);
                detectBreakout(newCandles, stock, res);
//                    newCandles.remove(0);
            }
        }
    }

    public void detectBreakout(List<Candle> candles, String stock, StockResponse res) {
        int lookback = 10;
        if (candles.size() <= lookback) {
//            System.out.println("Not enough data.");
            return;
        }

        // Get last 10 candles for consolidation range
        List<Candle> recent = candles.subList(candles.size() - lookback - 1, candles.size() - 1);

        double rangeHigh = recent.stream().mapToDouble(Candle::getHigh).max().orElse(0);
        double rangeLow = recent.stream().mapToDouble(Candle::getLow).min().orElse(0);
        double range = rangeHigh - rangeLow;
        double priceNow = candles.get(candles.size() - 1).getClose();

        // Threshold: tight range < 1.5% of current price
        if ((range / priceNow) > 0.015) {
//            System.out.println("No consolidation detected.");
            return;
        }

        // Check breakout on latest candle
        Candle lastCandle = candles.get(candles.size() - 1);
        double avgVol = recent.stream().mapToDouble(Candle::getVolume).average().orElse(0);

        if (lastCandle.getClose() > rangeHigh && lastCandle.getVolume() > avgVol && "LBU".equals(lastCandle.getOiInt()) && lastCandle.isHighVolume()) {
            log.info("Bullish Breakout Detected! {} OI {} Entry: {} Stop-loss: {} at {}", stock, lastCandle.getOiInt(), String.format("%.2f", rangeHigh + 0.002 * priceNow), rangeLow, lastCandle.getEndTime());
        } else if (lastCandle.getClose() < rangeLow && lastCandle.getVolume() > avgVol && "SBU".equals(lastCandle.getOiInt()) && lastCandle.isHighVolume()) {
            log.info("Bearish Breakdown Detected! {} OI {} Entry: {} Stop-loss: {} at {}", stock, lastCandle.getOiInt(), String.format("%.2f", rangeLow - 0.002 * priceNow), rangeHigh, lastCandle.getEndTime());
        } else {
//            System.out.println("No breakout yet.");
        }
        log.info("{} OI {} Entry: {} Stop-loss: {} at {}", stock, lastCandle.getOiInt(), String.format("%.2f", rangeHigh + 0.002 * priceNow), rangeLow, lastCandle.getEndTime());

    }
}