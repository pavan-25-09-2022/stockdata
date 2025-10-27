package com.stocks.entity;

import lombok.Data;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
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

	@OneToMany(mappedBy = "tradeSetup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<TargetInfoEntity> targetInfos;

	@OneToMany(mappedBy = "tradeSetup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<StopLossInfoEntity> stopLossInfos;

	@OneToMany(mappedBy = "tradeSetup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<EntryInfoEntity> entryInfos;

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
	private String criteria;
    private Double highestCeOIChangeStrike;
    private Double lowestCeOIChangeStrike;
    private Double highestPeOIChangeStrike;
    private Double lowestPeOIChangeStrike;
    private Double highestCeVolumeStrike;
    private Double highestPeVolumeStrike;


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
