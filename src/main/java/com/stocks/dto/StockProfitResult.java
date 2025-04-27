package com.stocks.dto;

public class StockProfitResult {
    private double total;
    private double profit;
    private double sellPrice;
    private double buyPrice;
    private String buyTime;
    private String sellTime;

    public StockProfitResult(double profit, double sellPrice, double buyPrice, String buyTime, String sellTime) {
        this.profit = profit;
        this.sellPrice = sellPrice;
        this.buyPrice = buyPrice;
        this.buyTime = buyTime;
        this.sellTime = sellTime;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getProfit() {
        return profit;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public String getBuyTime() {
        return buyTime;
    }

    public void setBuyTime(String buyTime) {
        this.buyTime = buyTime;
    }

    public String getSellTime() {
        return sellTime;
    }

    public void setSellTime(String sellTime) {
        this.sellTime = sellTime;
    }
}