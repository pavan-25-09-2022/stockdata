package com.stocks.dto;

public class UnderLyingAssetData {
    private String stUnderLyingAsset;
    private String stDateTime;
    private double inLtp;
    private double inDayHigh;
    private double inDayLow;
    private double inDayOpen;

    // Getters and Setters
    public String getStUnderLyingAsset() {
        return stUnderLyingAsset;
    }

    public void setStUnderLyingAsset(String stUnderLyingAsset) {
        this.stUnderLyingAsset = stUnderLyingAsset;
    }

    public String getStDateTime() {
        return stDateTime;
    }

    public void setStDateTime(String stDateTime) {
        this.stDateTime = stDateTime;
    }

    public double getInLtp() {
        return inLtp;
    }

    public void setInLtp(double inLtp) {
        this.inLtp = inLtp;
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

    public double getInDayOpen() {
        return inDayOpen;
    }

    public void setInDayOpen(double inDayOpen) {
        this.inDayOpen = inDayOpen;
    }
}