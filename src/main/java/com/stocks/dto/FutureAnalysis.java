package com.stocks.dto;


public class FutureAnalysis {

    String symbol;

    String duration;

    Double totalOI;

    Double totalChangeInOI;

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

    private HistoricalQuote historicalQuote;

    public FutureAnalysis(String symbol, String duration, Double totalOI, Double totalChangeInOI, Double dayHigh, Double dayLow, Double close, Double high, Double low, Double open, Long oiChange, String interpretation, String levelBreak, Double ltpChange, Long volume, boolean highVolume, double strength) {
       this.symbol = symbol;
        this.duration = duration;
        this.totalOI = totalOI;
        this.totalChangeInOI = totalChangeInOI;
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

    public FutureAnalysis(String symbol,String duration, Double totalOI, Double totalChangeInOI, Double dayHigh, Double dayLow, Double close, Double high, Double low, Double open, Long oiChange, String interpretation, String levelBreak, Double ltpChange, Long volume, boolean highVolume, double strength, double percentageChange) {
        this.symbol=symbol;
        this.duration = duration;
        this.totalOI = totalOI;
        this.totalChangeInOI = totalChangeInOI;
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
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public Long getOiChange() {
        return oiChange;
    }

    public void setOiChange(Long oiChange) {
        this.oiChange = oiChange;
    }

    public Double getOpen() {
        return open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Double getLow() {
        return low;
    }

    public void setLow(Double low) {
        this.low = low;
    }

    public Double getHigh() {
        return high;
    }

    public void setHigh(Double high) {
        this.high = high;
    }

    public Double getClose() {
        return close;
    }

    public void setClose(Double close) {
        this.close = close;
    }

    public Double getDayLow() {
        return dayLow;
    }

    public void setDayLow(Double dayLow) {
        this.dayLow = dayLow;
    }

    public Double getDayHigh() {
        return dayHigh;
    }

    public void setDayHigh(Double dayHigh) {
        this.dayHigh = dayHigh;
    }

    public Double getTotalChangeInOI() {
        return totalChangeInOI;
    }

    public void setTotalChangeInOI(Double totalChangeInOI) {
        this.totalChangeInOI = totalChangeInOI;
    }

    public Double getTotalOI() {
        return totalOI;
    }

    public void setTotalOI(Double totalOI) {
        this.totalOI = totalOI;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getLevelBreak() {
        return levelBreak;
    }

    public void setLevelBreak(String levelBreak) {
        this.levelBreak = levelBreak;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public boolean isHighVolume() {
        return highVolume;
    }

    public void setHighVolume(boolean highVolume) {
        this.highVolume = highVolume;
    }

    public Double getLtpChange() {
        return ltpChange;
    }

    public void setLtpChange(Double ltpChange) {
        this.ltpChange = ltpChange;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public HistoricalQuote getHistoricalQuote() {
        return historicalQuote;
    }

    public void setHistoricalQuote(HistoricalQuote historicalQuote) {
        this.historicalQuote = historicalQuote;
    }

    public Double getOiPercentageChange() {
        return oiPercentageChange;
    }

    public void setOiPercentageChange(Double oiPercentageChange) {
        this.oiPercentageChange = oiPercentageChange;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return "FutureAnalysis{" +
                "symbol='" + symbol + '\'' +
                ", duration='" + duration + '\'' +
                ", totalOI=" + totalOI +
                ", totalChangeInOI=" + totalChangeInOI +
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
