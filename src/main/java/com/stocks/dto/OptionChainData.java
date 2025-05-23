package com.stocks.dto;

public class OptionChainData {
    private String stFetchDate;
    private int inStrikePrice;
    private String stOptionsType;
    private int inTradedVolume;
    private int inOldOi;
    private double inOldIv;
    private double inOldClose;
    private int inNewOi;
    private double inNewClose;
    private double inNewIv;
    private double inNewDayOpen;
    private double inNewDayHigh;
    private double inNewDayLow;
    private String stExpiryDate;


    // Getters and Setters
    public String getStFetchDate() {
        return stFetchDate;
    }

    public void setStFetchDate(String stFetchDate) {
        this.stFetchDate = stFetchDate;
    }

    public int getInStrikePrice() {
        return inStrikePrice;
    }

    public void setInStrikePrice(int inStrikePrice) {
        this.inStrikePrice = inStrikePrice;
    }

    public String getStOptionsType() {
        return stOptionsType;
    }

    public void setStOptionsType(String stOptionsType) {
        this.stOptionsType = stOptionsType;
    }

    public int getInTradedVolume() {
        return inTradedVolume;
    }

    public void setInTradedVolume(int inTradedVolume) {
        this.inTradedVolume = inTradedVolume;
    }

    public int getInOldOi() {
        return inOldOi;
    }

    public void setInOldOi(int inOldOi) {
        this.inOldOi = inOldOi;
    }

    public double getInOldIv() {
        return inOldIv;
    }

    public void setInOldIv(double inOldIv) {
        this.inOldIv = inOldIv;
    }

    public double getInOldClose() {
        return inOldClose;
    }

    public void setInOldClose(double inOldClose) {
        this.inOldClose = inOldClose;
    }

    public int getInNewOi() {
        return inNewOi;
    }

    public void setInNewOi(int inNewOi) {
        this.inNewOi = inNewOi;
    }

    public double getInNewClose() {
        return inNewClose;
    }

    public void setInNewClose(double inNewClose) {
        this.inNewClose = inNewClose;
    }

    public double getInNewIv() {
        return inNewIv;
    }

    public void setInNewIv(double inNewIv) {
        this.inNewIv = inNewIv;
    }

    public double getInNewDayOpen() {
        return inNewDayOpen;
    }

    public void setInNewDayOpen(double inNewDayOpen) {
        this.inNewDayOpen = inNewDayOpen;
    }

    public double getInNewDayHigh() {
        return inNewDayHigh;
    }

    public void setInNewDayHigh(double inNewDayHigh) {
        this.inNewDayHigh = inNewDayHigh;
    }

    public double getInNewDayLow() {
        return inNewDayLow;
    }

    public void setInNewDayLow(double inNewDayLow) {
        this.inNewDayLow = inNewDayLow;
    }

    public String getStExpiryDate() {
        return stExpiryDate;
    }

    public void setStExpiryDate(String stExpiryDate) {
        this.stExpiryDate = stExpiryDate;
    }

    public int getOIChange(){
        return  inNewOi-inOldOi;
    }
}