package com.stocks.dto;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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
	private double entryPrice1;
	private double entryPrice2;
	private double stopLoss;
	private double targetPrice1;
	private double targetPrice2;
	private double averagePrice;
	private String criteria;

	public StockData(String s, String p) {
		this.stock = s;
		this.type = p;
	}

	public StockData() {

	}


}