package com.stocks.service;

import com.stocks.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandlePatternEvaluator {

    @Autowired
    private CommonValidation commonValidation;
    @Autowired
    private CandlePattern candlePattern;

    public StockResponse evaluateBullishPattern(String stock, Properties properties) {
        List<Candle> candles = commonValidation.getCandles(properties, stock);
        if (candles == null || candles.size() < 2) return null;

        for (int i = 1; i < candles.size(); i++) {
            Candle prev = candles.get(i - 1);
            Candle cur = candles.get(i);
            PatternType pattern = candlePattern.evaluate(candles);

            if (isBullishPattern(pattern)) {
                StockResponse res = new StockResponse();
                res.setStock(stock);
                res.setCurCandle(cur);
                res.setFirstCandle(candles.get(0));
                return res;
            }
        }
        return null;
    }

    private boolean isBullishPattern(PatternType pattern) {
        return pattern == PatternType.BULLISH_ENGULFING ||
               pattern == PatternType.HAMMER ||
               pattern == PatternType.MORNING_STAR ||
               pattern == PatternType.DRAGONFLY_DOJI ||
               pattern == PatternType.TWEEZER_BOTTOM ||
               pattern == PatternType.MARUBOZU; // Add more as needed
    }
}