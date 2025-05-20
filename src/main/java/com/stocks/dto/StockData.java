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
}