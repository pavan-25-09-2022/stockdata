package com.stocks.dto;


import javax.persistence.*;

@Entity
@Table(name = "stock_data")
public class StockData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stock;
    private String date;
    private String time;
    private String oiInterpretation;
    private String type;
    private String trend;
    private Integer entryPrice1;
    private Integer entryPrice2;
    private Integer stopLoss;
    private Integer targetPrice1;
    private Integer targetPrice2;
    private Integer averagePrice;

    public StockData(String s, String p) {
        this.stock = s;
        this.type = p;
    }

    public StockData() {

    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public Integer getEntryPrice1() {
        return entryPrice1;
    }

    public void setEntryPrice1(Integer entryPrice1) {
        this.entryPrice1 = entryPrice1;
    }

    public Integer getEntryPrice2() {
        return entryPrice2;
    }

    public void setEntryPrice2(Integer entryPrice2) {
        this.entryPrice2 = entryPrice2;
    }

    public Integer getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(Integer stopLoss) {
        this.stopLoss = stopLoss;
    }


    public Integer getTargetPrice1() {
        return targetPrice1;
    }

    public void setTargetPrice1(Integer targetPrice1) {
        this.targetPrice1 = targetPrice1;
    }

    public Integer getTargetPrice2() {
        return targetPrice2;
    }

    public void setTargetPrice2(Integer targetPrice2) {
        this.targetPrice2 = targetPrice2;
    }

    public Integer getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(Integer averagePrice) {
        this.averagePrice = averagePrice;
    }
}