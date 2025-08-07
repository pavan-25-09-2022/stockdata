package com.stocks.dto;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "option_chain_data")
@Data
public class OptionChainData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Foreign key to StockData
	@ManyToOne
	@JoinColumn(name = "stock_data_id", referencedColumnName = "id")
	private StockData stockData;

	private String stFetchDate;
	private double inStrikePrice;
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

	public int getOIChange() {
		return inNewOi - inOldOi;
	}

}