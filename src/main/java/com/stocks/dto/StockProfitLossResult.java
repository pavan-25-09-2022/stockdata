package com.stocks.dto;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "stock_profit_loss_result")
@Data
public class StockProfitLossResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stock;
    private String date;
    private double total;
    private double quantity;
    private double sellPrice;
    private double buyPrice;
    private LocalDateTime buyTime;
    private LocalDateTime sellTime;
    private String type;


}
