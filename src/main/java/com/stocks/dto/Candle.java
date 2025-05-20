package com.stocks.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
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
}
