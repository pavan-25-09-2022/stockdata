package com.stocks.dto;

public class StockResponse {

    private String stock;
    private String time;
    private String oiInterpretation;
    private double stopLoss;
    private double currentPrice;
    boolean isHighVolume;
    String stockType;
    String eodData;
    StockProfitResult stockProfitResult;
    int priority;

    public StockResponse(String stock, String stockType, String time, String oiInterpretation, double stopLoss, double currentPrice, boolean isHighVolume) {
        this.stock = stock;
        this.time = time;
        this.oiInterpretation = oiInterpretation;
        this.stopLoss = stopLoss;
        this.currentPrice = currentPrice;
        this.isHighVolume = isHighVolume;
        this.stockType = stockType;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
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

    public boolean isHighVolume() {
        return isHighVolume;
    }

    public void setHighVolume(boolean highVolume) {
        isHighVolume = highVolume;
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
}
