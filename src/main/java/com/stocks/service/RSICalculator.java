package com.stocks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RSICalculator {
    private static final Logger log = LoggerFactory.getLogger(RSICalculator.class);

    public double calculateRSI(List<Double> closePrices, int period) {
       if (closePrices.size() >= period) {
//           throw new IllegalArgumentException("Need more than " + period + " data points");
           double gainSum = 0;
           double lossSum = 0;
           // Initial average gain/loss
           for (int i = 1; i < period; i++) {
               double change = closePrices.get(i) - closePrices.get(i - 1);
               if (change > 0) {
                   gainSum += change;
               } else {
                   lossSum -= change; // Make it positive
               }
           }
           double avgGain = gainSum / period;
           double avgLoss = lossSum / period;
           // If avgLoss is zero, RSI is 100 (no losses)
           if (avgLoss == 0) {
               return 100.0;
           }
           double rs = avgGain / avgLoss;
           double value = 100 - (100 / (1 + rs));
//           log.info("RSI value: " + value + " for period: " + period);
           return value;
       }
       return 0.0;
   }
//   public static void main(String[] args) {
//       // Example 3-minute candlestick close prices (15 periods)
//       List<Double> closePrices = Arrays.asList(
//           100.0, 101.2, 100.8, 101.5, 101.0,
//           101.3, 101.7, 101.2, 101.4, 101.8,
//           102.0, 101.5, 101.7, 101.2, 101.5
//       );
//       int period = 14;
//       try {
//           double rsi = calculateRSI(closePrices, period);
//           System.out.printf("RSI (14-period): %.2f\n", rsi);
//       } catch (IllegalArgumentException e) {
//           System.out.println("Error: " + e.getMessage());
//       }
//   }
}