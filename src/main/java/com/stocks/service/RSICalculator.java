package com.stocks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RSICalculator {
    private static final Logger log = LoggerFactory.getLogger(RSICalculator.class);

    public double calculateRSI(List<Double> closePrices, int period) {
        if (closePrices == null || closePrices.size() <= period) {
            throw new IllegalArgumentException("Need more than " + period + " data points");
        }
        double gainSum = 0;
        double lossSum = 0;
        // Initial average gain/loss
        for (int i = 1; i <= period; i++) {
            double change = closePrices.get(i) - closePrices.get(i - 1);
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum -= change; // Make it positive
            }
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        if (avgLoss == 0) {
            return 100.0;
        }
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}