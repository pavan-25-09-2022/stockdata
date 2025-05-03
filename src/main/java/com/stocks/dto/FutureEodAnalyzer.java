package com.stocks.dto;

public class FutureEodAnalyzer {
    
    private String stFetchDate;
    private double inDayOpen;
    private double inDayHigh;
    private double inDayLow;
    private double inClose;
    private String inOi;
    private long inVolume;
    private long inDelQty;
    private double ltpChange;
    private long oiChange;
    private String oiInterpretation;
    private double strength;

    public String getStFetchDate() {
        return stFetchDate;
    }

    public void setStFetchDate(String stFetchDate) {
        this.stFetchDate = stFetchDate;
    }

    public double getInDayOpen() {
        return inDayOpen;
    }

    public void setInDayOpen(double inDayOpen) {
        this.inDayOpen = inDayOpen;
    }

    public double getInDayHigh() {
        return inDayHigh;
    }

    public void setInDayHigh(double inDayHigh) {
        this.inDayHigh = inDayHigh;
    }

    public double getInDayLow() {
        return inDayLow;
    }

    public void setInDayLow(double inDayLow) {
        this.inDayLow = inDayLow;
    }

    public double getInClose() {
        return inClose;
    }

    public void setInClose(double inClose) {
        this.inClose = inClose;
    }

    public String getInOi() {
        return inOi;
    }

    public void setInOi(String inOi) {
        this.inOi = inOi;
    }

    public long getInVolume() {
        return inVolume;
    }

    public void setInVolume(long inVolume) {
        this.inVolume = inVolume;
    }

    public long getInDelQty() {
        return inDelQty;
    }

    public void setInDelQty(long inDelQty) {
        this.inDelQty = inDelQty;
    }

    public double getLtpChange() {
        return ltpChange;
    }

    public void setLtpChange(double ltpChange) {
        this.ltpChange = ltpChange;
    }

    public String getOiInterpretation() {
        return oiInterpretation;
    }

    public void setOiInterpretation(String oiInterpretation) {
        this.oiInterpretation = oiInterpretation;
    }

    public long getOiChange() {
        return oiChange;
    }

    public void setOiChange(long oiChange) {
        this.oiChange = oiChange;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    @Override
    public String toString() {
        return "FutureEodAnalyzer{" +
                "stFetchDate='" + stFetchDate + '\'' +
                ", inDayOpen=" + inDayOpen +
                ", inDayHigh=" + inDayHigh +
                ", inDayLow=" + inDayLow +
                ", inClose=" + inClose +
                ", inOi='" + inOi + '\'' +
                ", inVolume=" + inVolume +
                ", inDelQty=" + inDelQty +
                ", ltpChange=" + ltpChange +
                ", oiChange=" + oiChange +
                ", oiInterpretation='" + oiInterpretation + '\'' +
                ", strength=" + strength +
                '}';
    }
}
