package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.PatternType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandlePattern {

    public PatternType evaluate(List<Candle> candles) {
        if (candles == null || candles.isEmpty()) return PatternType.NONE;

        Candle candle = candles.get(0);
        Candle prevCandle = candles.size() > 1 ? candles.get(1) : null;

        double open = candle.getOpen();
        double close = candle.getClose();
        double high = candle.getHigh();
        double low = candle.getLow();
        double body = Math.abs(open - close);
        double upperWick = Math.max(0, high - Math.max(open, close));
        double lowerWick = Math.max(0, Math.min(open, close) - low);
        double range = high - low;
        if (range == 0) return PatternType.NONE;

        if (isShootingStar(body, upperWick, lowerWick, open, close, low, range)) return PatternType.SHOOTING_STAR;
        if (isThreeBlackCrows(candles)) return PatternType.THREE_BLACK_CROWS;

        if (isDoji(body, range)) return PatternType.DOJI;
        if (isHammer(body, lowerWick, upperWick, open, close, low, range)) return PatternType.HAMMER;
        if (isInvertedHammer(body, upperWick, lowerWick, open, close, low, range)) return PatternType.INVERTED_HAMMER;
        if (isMarubozu(body, upperWick, lowerWick, range)) return PatternType.MARUBOZU;
        if (isSpinningTop(body, upperWick, lowerWick, range)) return PatternType.SPINNING_TOP;

        if (prevCandle != null) {
            if (isBullishEngulfing(candle, prevCandle)) return PatternType.BULLISH_ENGULFING;
            if (isBearishEngulfing(candle, prevCandle)) return PatternType.BEARISH_ENGULFING;
            if (isMorningStar(candle, prevCandle, body, range)) return PatternType.MORNING_STAR;
            if (isEveningStar(candle, prevCandle, body, range)) return PatternType.EVENING_STAR;
            if (isTweezerTop(candle, prevCandle, high, range)) return PatternType.TWEEZER_TOP;
            if (isTweezerBottom(candle, prevCandle, low, range)) return PatternType.TWEEZER_BOTTOM;
        }

        return PatternType.NONE;
    }

    private boolean isDoji(double body, double range) {
        return body <= 0.1 * range;
    }

    private boolean isHammer(double body, double lowerWick, double upperWick, double open, double close, double low, double range) {
        return body > 0 && lowerWick >= 2 * body && upperWick <= body * 0.3 && Math.max(open, close) > (low + 0.6 * range);
    }

    private boolean isInvertedHammer(double body, double upperWick, double lowerWick, double open, double close, double low, double range) {
        return body > 0 && upperWick >= 2 * body && lowerWick <= body * 0.3 && Math.min(open, close) < (low + 0.4 * range);
    }

    private boolean isMarubozu(double body, double upperWick, double lowerWick, double range) {
        return body >= 0.9 * range && upperWick <= 0.05 * range && lowerWick <= 0.05 * range;
    }

    private boolean isSpinningTop(double body, double upperWick, double lowerWick, double range) {
        return body <= 0.3 * range && upperWick > 0.3 * range && lowerWick > 0.3 * range;
    }

    private boolean isBullishEngulfing(Candle candle, Candle prev) {
        double open = candle.getOpen(), close = candle.getClose();
        double prevOpen = prev.getOpen(), prevClose = prev.getClose();
        double body = Math.abs(open - close), prevBody = Math.abs(prevOpen - prevClose);
        return close > open && prevClose < prevOpen && close > prevOpen && open < prevClose && body > prevBody;
    }

    private boolean isBearishEngulfing(Candle candle, Candle prev) {
        double open = candle.getOpen(), close = candle.getClose();
        double prevOpen = prev.getOpen(), prevClose = prev.getClose();
        double body = Math.abs(open - close), prevBody = Math.abs(prevOpen - prevClose);
        return open > close && prevOpen < prevClose && open > prevClose && close < prevOpen && body > prevBody;
    }

    private boolean isMorningStar(Candle candle, Candle prev, double body, double range) {
        double open = candle.getOpen(), close = candle.getClose();
        double prevOpen = prev.getOpen(), prevClose = prev.getClose();
        return prevClose < prevOpen && close > open && close > prevOpen && body > 0.2 * range;
    }

    private boolean isEveningStar(Candle candle, Candle prev, double body, double range) {
        double open = candle.getOpen(), close = candle.getClose();
        double prevOpen = prev.getOpen(), prevClose = prev.getClose();
        return prevOpen < prevClose && open > close && close < prevOpen && body > 0.2 * range;
    }

    private boolean isTweezerTop(Candle candle, Candle prev, double high, double range) {
        double open = candle.getOpen(), close = candle.getClose();
        double prevOpen = prev.getOpen(), prevClose = prev.getClose();
        return prevOpen < prevClose && open > close && Math.abs(prev.getHigh() - high) < 0.1 * range;
    }

    private boolean isTweezerBottom(Candle candle, Candle prev, double low, double range) {
        double open = candle.getOpen(), close = candle.getClose();
        double prevOpen = prev.getOpen(), prevClose = prev.getClose();
        return prevClose < prevOpen && close > open && Math.abs(prev.getLow() - low) < 0.1 * range;
    }

    private boolean isShootingStar(double body, double upperWick, double lowerWick, double open, double close, double low, double range) {
        return body > 0 && upperWick >= 2 * body && lowerWick <= body * 0.3 && Math.min(open, close) < (low + 0.4 * range);
    }

    public boolean isThreeBlackCrows(List<Candle> candles) {
        if (candles.size() < 3) return false;
        Candle c1 = candles.get(2);
        Candle c2 = candles.get(1);
        Candle c3 = candles.get(0);

        return c1.isBearish() && c2.isBearish() && c3.isBearish() &&
                c2.getOpen() < c1.getClose() && c3.getOpen() < c2.getClose() &&
                c1.getClose() > c2.getClose() && c2.getClose() > c3.getClose();
    }
}