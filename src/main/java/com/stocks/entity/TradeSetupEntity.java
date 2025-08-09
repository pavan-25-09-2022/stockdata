package com.stocks.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "trade_setup")
@Data
public class TradeSetupEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToMany(mappedBy = "tradeSetup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<StrikeSetupEntity> strikeSetups;

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
	private String tradeNotes;
	private String entry1Time;
	private String entry2Time;
	private String target1Time;
	private String target2Time;
	private String stopLoss1Time;
	private String strategy;
	private String type;

	@Override
	public String toString() {
		return "TradeSetupEntity{" +
				"id=" + id +
				", stockSymbol='" + stockSymbol + '\'' +
				", stockDate='" + stockDate + '\'' +
				// do not include strikeSetups or include only their IDs/count
				'}';
	}
}
