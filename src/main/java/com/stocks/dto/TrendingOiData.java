// DataItem.java
package com.stocks.dto;

import java.util.List;

public class TrendingOiData {
    private String stFetchDate;
    private String stTime;
    private String stDataFetchType;
    private double inClose;
    private double inHigh;
    private double inLow;
    private long totalCeOi;
    private long totalPeOi;

    // Getters and setters
    public String getStFetchDate() {
        return stFetchDate;
    }

    public void setStFetchDate(String stFetchDate) {
        this.stFetchDate = stFetchDate;
    }

    public String getStTime() {
        return stTime;
    }

    public void setStTime(String stTime) {
        this.stTime = stTime;
    }

    public String getStDataFetchType() {
        return stDataFetchType;
    }

    public void setStDataFetchType(String stDataFetchType) {
        this.stDataFetchType = stDataFetchType;
    }

    public double getInClose() {
        return inClose;
    }

    public void setInClose(double inClose) {
        this.inClose = inClose;
    }

    public double getInHigh() {
        return inHigh;
    }

    public void setInHigh(double inHigh) {
        this.inHigh = inHigh;
    }

    public double getInLow() {
        return inLow;
    }

    public void setInLow(double inLow) {
        this.inLow = inLow;
    }

    public long getTotalCeOi() {
        return totalCeOi;
    }

    public void setTotalCeOi(long totalCeOi) {
        this.totalCeOi = totalCeOi;
    }

    public long getTotalPeOi() {
        return totalPeOi;
    }

    public void setTotalPeOi(long totalPeOi) {
        this.totalPeOi = totalPeOi;
    }
}