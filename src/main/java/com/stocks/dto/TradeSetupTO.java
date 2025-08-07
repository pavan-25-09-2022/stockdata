package com.stocks.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class TradeSetupTO {
	private String stockSymbol;
	private String Date;
	private String fetchTime;
	private Double oiChgPer;
	private Double ltpChgPer;
	private Double entry1;
	private Double entry2;
	private Double target1;
	private Double target2;
	private Double stopLoss1;
	private Double stopLoss2;
	private String status;
	private String tradeNotes;
	private String entry1Time;
	private String entry2Time;
	private String target1Time;
	private String target2Time;
	private String stopLoss1Time;
	private String strategy;
	private String type;

	Map<Integer, StrikeTO> strikes;
}
