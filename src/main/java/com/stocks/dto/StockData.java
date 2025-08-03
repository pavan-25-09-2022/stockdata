package com.stocks.dto;


import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "stock_data")
@Data
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
    private String criteria;

    public StockData(String s, String p) {
        this.stock = s;
        this.type = p;
    }

    public StockData() {

    }


}