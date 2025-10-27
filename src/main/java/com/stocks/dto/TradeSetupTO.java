package com.stocks.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class TradeSetupTO {
	private String stockSymbol;
	private String stockDate;
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
	private String tradeNotes = "";
	private String entry1Time;
	private String entry2Time;
	private String target1Time;
	private String target2Time;
	private String stopLoss1Time;
	private String strategy;
	private String type;
	private String criteria;


	private List<TargetInfo> targetInfos = new ArrayList<>();
	private List<StopLossInfo> stopLossInfos = new ArrayList<>();
	private List<EntryInfo> entryInfos = new ArrayList<>();
    private Double highestCeOIChangeStrike;
    private Double lowestCeOIChangeStrike;
    private Double highestPeOIChangeStrike;
    private Double lowestPeOIChangeStrike;
    private Double highestCeVolumeStrike;
    private Double highestPeVolumeStrike;

	Map<Integer, StrikeTO> strikes;

	public void setTradeNotes(String note) {
		if (this.tradeNotes == null || this.tradeNotes.isEmpty()) {
			this.tradeNotes = note;
		} else {
			this.tradeNotes += " | " + note;
		}
	}

	public void setCriteria(String note) {
		if (this.criteria == null || this.criteria.isEmpty()) {
			this.criteria = note;
		} else {
			this.criteria += " | " + note;
		}
	}
}
