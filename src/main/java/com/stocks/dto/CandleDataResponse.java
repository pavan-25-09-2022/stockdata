package com.stocks.dto;

import lombok.Data;

import java.util.List;

@Data
public class CandleDataResponse {

	private String status;
	private String msg;
	private List<Data> data;
	private String nextTime;

	@lombok.Data
	public static class Data {
		private String time;
		private double open;
		private double high;
		private double low;
		private double close;
		private long volume;
		private String oi;
	}
}