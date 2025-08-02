package com.stocks.dto;

import lombok.Data;

@Data
public class Properties {
    private String stockDate;
    private int exitMins;
    private int interval;
    private boolean fetchAll;
    private int amtInvested;
    private String stockName = "";
    private String fileName;
    private String startTime;
    private String endTime;
    private String expiryDate;
    private String type;
    private String env;
    private String previousStockDate;
    private boolean fromScheduler;
    private Integer noOfCandles;
    private boolean checkRecentCandle;
    private boolean withVolume;

    public String getStockDate() {
        return stockDate;
    }

    public void setStockDate(String stockDate) {
        this.stockDate = stockDate;
    }

    public int getExitMins() {
        return exitMins;
    }

    public void setExitMins(int exitMins) {
        this.exitMins = exitMins;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public boolean isFetchAll() {
        return fetchAll;
    }

    public void setFetchAll(boolean fetchAll) {
        this.fetchAll = fetchAll;
    }

    public int getAmtInvested() {
        return amtInvested;
    }

    public void setAmtInvested(int amtInvested) {
        this.amtInvested = amtInvested;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getPreviousStockDate() {
        return previousStockDate;
    }

    public void setPreviousStockDate(String previousStockDate) {
        this.previousStockDate = previousStockDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;}

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isFromScheduler() {
        return fromScheduler;
    }

    public void setFromScheduler(boolean fromScheduler) {
        this.fromScheduler = fromScheduler;
    }

    public Integer getNoOfCandles() {
        return noOfCandles;
    }

    public void setNoOfCandles(Integer noOfCandles) {
        this.noOfCandles = noOfCandles;
    }

    public boolean isCheckRecentCandle() {
        return checkRecentCandle;
    }

    public void setCheckRecentCandle(boolean checkRecentCandle) {
        this.checkRecentCandle = checkRecentCandle;
    }

    public boolean isWithVolume() {
        return withVolume;
    }

    public void setWithVolume(boolean withVolume) {
        this.withVolume = withVolume;
    }
}
