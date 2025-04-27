package com.stocks.dto;

public class StockEODDataResponse {
    
    private String stFetchDate;
    private double inDayOpen;
    private double inDayHigh;
    private double inDayLow;
    private double inClose;
    private String inOi;
    private long inVolume;
    private long inDelQty;

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
}
