package com.stocks.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "entry_info")
@Data
@NoArgsConstructor
public class EntryInfoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trade_setup_id")
	private TradeSetupEntity tradeSetup;

	private String strategy;
	private Double entryPrice;
	private String entryTime;

	public EntryInfoEntity(String strategy, Double entryPrice) {
		this.strategy = strategy;
		this.entryPrice = entryPrice != null ? Math.round(entryPrice * 100.0) / 100.0 : null;
	}
}
