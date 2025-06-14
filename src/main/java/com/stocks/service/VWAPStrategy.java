package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.StockResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class VWAPStrategy {

    public double calculateVWAP(List<Candle> candles) {
        double pvSum = 0.0, volSum = 0.0;
        for (Candle c : candles) {
            double tp = c.typicalPrice();
            pvSum += tp * c.getVolume();
            volSum += c.getVolume();
        }
        return (volSum == 0) ? 0 : pvSum / volSum;
    }

    public double calculateRSI(List<Double> closes, int period) {
        if (closes.size() <= period) return 0;

        double gain = 0, loss = 0;
        for (int i = 1; i <= period; i++) {
            double change = closes.get(i) - closes.get(i - 1);
            if (change > 0) gain += change;
            else loss += -change;
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        for (int i = period + 1; i < closes.size(); i++) {
            double change = closes.get(i) - closes.get(i - 1);
            if (change > 0) {
                avgGain = (avgGain * (period - 1) + change) / period;
                avgLoss = (avgLoss * (period - 1)) / period;
            } else {
                avgGain = (avgGain * (period - 1)) / period;
                avgLoss = (avgLoss * (period - 1) + -change) / period;
            }
        }

        if (avgLoss == 0) return 100;

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    public double calculateATR(List<Candle> candles, int period) {
        if (candles.size() < period + 1) return 0.0;
        double atrSum = 0.0;
        for (int i = candles.size() - period; i < candles.size(); i++) {
            Candle cur = candles.get(i);
            Candle prev = candles.get(i - 1);
            double tr = Math.max(
                    cur.getHigh() - cur.getLow(),
                    Math.max(
                            Math.abs(cur.getHigh() - prev.getClose()),
                            Math.abs(cur.getLow() - prev.getClose())
                    )
            );
            atrSum += tr;
        }
        return atrSum / period;
    }

    public void checkExitSignal(StockResponse res, List<Candle> candles) {
        if("N".equals(res.getStockType())){
            log.info("Nifty strategy not implemented yet");
            return;
        }
        double entryPrice = res.getCurrentPrice();
        double stopLoss = res.getStopLoss();
        double targetPrice = res.getCurrentPrice() * 1.011;
        List<Double> closePrices = candles.stream()
                .map(Candle::getClose)
                .collect(Collectors.toList());
        double vwap = calculateVWAP(candles);
        double rsi = calculateRSI(closePrices, 14);
        Candle cur = candles.get(candles.size() - 1);
        double currentPrice = cur.getClose();

        if (currentPrice <= stopLoss) {
            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", vwap), String.format("%.2f", rsi),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
            log.info("🚨 Exit — Stop-loss hit at {} is positive {} ", cur.getEndTime(),res.getCurrentPrice()<currentPrice);
//            res.setStock(null);
        } else if (currentPrice >= targetPrice) {
            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", vwap), String.format("%.2f", rsi),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
            log.info("🎯 Exit — Target reached at {} is positive {} ", cur.getEndTime(),res.getCurrentPrice()<currentPrice);
            res.setStock(null);
        }
//        else if (rsi > 80) {
//            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", vwap), String.format("%.2f", rsi),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
//            log.info("📉 Exit — RSI Overbought + Below VWAP (Weakness) at {} is positive {} ", cur.getEndTime(),res.getCurrentPrice()<currentPrice);
//            res.setStock(null);
//        }
//        else if (rsi < 30) {
//            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", vwap), String.format("%.2f", rsi),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
//            log.info("⚠️ Exit — RSI Oversold bounce failing at {} is positive {} ", cur.getEndTime(),res.getCurrentPrice()<currentPrice);
////            res.setStock(null);
//        }
        if(res.getStock() != null && (cur.getEndTime().equals(LocalTime.of(15, 10)))
        ) {
            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", vwap), String.format("%.2f", rsi),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
            log.info("End of day reached, exiting strategy. is positive {} time {} ", res.getCurrentPrice()<currentPrice, cur.getEndTime());
            res.setStock(null);
        }
    }


}
