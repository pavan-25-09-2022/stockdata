package com.stocks.dto;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "future_analysis")
@Data
public class FutureAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    String symbol;

    String date;

    String duration;

    Double totalOI;

    Long totalDayChangeInOI;

    Double dayHigh;

    Double dayLow;

    Double close;

    Double high;

    Double low;

    Double open;

    Long oiChange ;

    String interpretation;

    String levelBreak;

    Double ltpChange;

    Long volume;

    boolean highVolume;

    double strength;

    Double oiPercentageChange;

    Double ltpPercentageChange;

    @Transient
    private HistoricalQuote historicalQuote;

    public FutureAnalysis() {
    }

    public FutureAnalysis(String symbol, String duration, Double totalOI, Long totalDayChangeInOI, Double dayHigh, Double dayLow, Double close, Double high, Double low, Double open, Long oiChange, String interpretation, String levelBreak, Double ltpChange, Long volume, boolean highVolume, double strength) {
       this.symbol = symbol;
        this.duration = duration;
        this.totalOI = totalOI;
        this.totalDayChangeInOI = totalDayChangeInOI;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.close = close;
        this.high = high;
        this.low = low;
        this.open = open;
        this.oiChange = oiChange;
        this.interpretation = interpretation;
        this.levelBreak = levelBreak;
        this.ltpChange = ltpChange;
        this.volume = volume;
        this.highVolume = highVolume;
        this.strength = strength;
    }

    public FutureAnalysis(String symbol,String date, String duration, Double totalOI, Long totalDayChangeInOI, Double dayHigh, Double dayLow, Double close, Double high, Double low, Double open, Long oiChange, String interpretation, String levelBreak, Double ltpChange, Long volume, boolean highVolume, double strength, double percentageChange, double ltpPercentageChange) {
        this.symbol=symbol;
        this.date = date;
        this.duration = duration;
        this.totalOI = totalOI;
        this.totalDayChangeInOI = totalDayChangeInOI;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.close = close;
        this.high = high;
        this.low = low;
        this.open = open;
        this.oiChange = oiChange;
        this.interpretation = interpretation;
        this.levelBreak = levelBreak;
        this.ltpChange = ltpChange;
        this.volume = volume;
        this.highVolume = highVolume;
        this.strength = strength;
        this.oiPercentageChange = percentageChange;
        this.ltpPercentageChange = ltpPercentageChange;
    }

    @Override
    public String toString() {
        return "FutureAnalysis{" +
                "symbol='" + symbol + '\'' +
                ", duration='" + duration + '\'' +
                ", totalOI=" + totalOI +
                ", totalDayChangeInOI=" + totalDayChangeInOI +
                ", dayHigh=" + dayHigh +
                ", dayLow=" + dayLow +
                ", close=" + close +
                ", high=" + high +
                ", low=" + low +
                ", open=" + open +
                ", oiChange=" + oiChange +
                ", interpretation='" + interpretation + '\'' +
                ", levelBreak='" + levelBreak + '\'' +
                ", ltpChange=" + ltpChange +
                ", volume=" + volume +
                ", highVolume=" + highVolume +
                ", strength=" + strength +
                ", percentageChange=" + oiPercentageChange +
                ", historicalQuote=" + historicalQuote +
                '}';
    }
}
