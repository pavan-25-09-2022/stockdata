package com.stocks.dto;

import com.stocks.utils.FormatUtil;

public class StockResponse {

    private String stock;
    private String startTime;
    private String endTime;
    private String oiInterpretation;
    private double stopLoss;
    private double currentPrice;
    private String volume;
    String stockType;
    String eodData;
    StockProfitResult stockProfitResult;
    int priority;


    public StockResponse(String stock, String stockType, String startTime, String time, String oiInterpretation, double stopLoss, double currentPrice, long volume) {
        this.stock = stock;
        this.startTime = FormatUtil.formatTime(startTime);
        this.endTime = FormatUtil.formatTime(time);
        this.oiInterpretation = oiInterpretation;
        this.stopLoss = stopLoss;
        this.currentPrice = currentPrice;
        this.stockType = stockType;
        this.volume = FormatUtil.formatVolume(volume);
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getOiInterpretation() {
        return oiInterpretation;
    }

    public void setOiInterpretation(String oiInterpretation) {
        this.oiInterpretation = oiInterpretation;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getStockType() {
        return stockType;
    }

    public void setStockType(String stockType) {
        this.stockType = stockType;
    }

    public StockProfitResult getStockProfitResult() {
        return stockProfitResult;
    }

    public void setStockProfitResult(StockProfitResult stockProfitResult) {
        this.stockProfitResult = stockProfitResult;
    }

    public String getEodData() {
        return eodData;
    }

    public void setEodData(String eodData) {
        this.eodData = eodData;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getVolume() {
        return volume;
    }

    public String setVolume(String volume) {
        this.volume = volume;
        return volume;
    }
}
