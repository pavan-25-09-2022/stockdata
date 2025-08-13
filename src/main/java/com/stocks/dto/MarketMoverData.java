package com.stocks.dto;

import lombok.Data;

@Data
public class MarketMoverData {

	private String stSymbolName;
	private String stFetchTime;
	private String inOldOi;
	private String inOldClose;
	private String inNewOi;
	private String inNewClose;
	private double inNewDayOpen;
	private double inNewDayHigh;
	private double inNewDayLow;

}
