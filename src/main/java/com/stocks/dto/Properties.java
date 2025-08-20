package com.stocks.dto;

import lombok.Data;

@Data
public class Properties {
	private String stockDate;
	private int exitMins;
	private int interval;
	private boolean fetchAll;
	private int amtInvested;
	private String stockName = "";
	private String fileName;
	private String startDate;
	private String endDate;
	private String startTime;
	private String endTime;
	private String expiryDate;
	private String type;
	private String env;
	private String previousStockDate;
	private boolean fromScheduler;
	private Integer noOfCandles;
	private boolean checkRecentCandle;
	private boolean withVolume;
	private String strategy;
}
