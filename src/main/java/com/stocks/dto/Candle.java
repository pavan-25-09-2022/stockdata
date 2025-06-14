package com.stocks.dto;

import com.stocks.service.CandlePattern;
import lombok.Data;
import lombok.ToString;

import java.time.LocalTime;

@Data
@ToString
public class Candle {
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private int count;
    private long openInterest;
    private boolean isHighVolume;
    private LocalTime startTime;
    private LocalTime endTime;
    private String oiInt;
    private long oiChange;
    private double curOiPer;
    private double totalOiPer;
    private double ltpChange;
    private double strength;
    private PatternType patternType;

    public double typicalPrice() {
        return (high + low + close) / 3.0;
    }

    public double getStrength() {
        return Math.abs(oiChange * ltpChange);
    }

    public double getCandleStrength() {
        return Math.abs(volume * getBody());
    }

    public double getBody() {
        return open - close;
    }

    public boolean isBullish() {
        return close > open;
    }

    public boolean isBearish() {
        return close < open;
    }

    public double bodySize() {
        return Math.abs(close - open);
    }
}
