package com.stocks.dto;

import lombok.Data;

import java.util.List;

@Data
public class FandOStocksTO {
	private String status;
	private String msg;
	private List<OptionData> data;

	// getters and setters
}

