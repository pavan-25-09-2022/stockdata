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
@Table(name = "stop_loss_info")
@Data
@NoArgsConstructor
public class StopLossInfoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trade_setup_id")
	private TradeSetupEntity tradeSetup;

	private String strategy;
	private Double stopLoss1;
	private String stopLoss1Time;
	private Integer stopLoss1ReachedAfter;
	private Double slWithE1Per;
	private Double slWithE2Per;
	private Double slWithE3Per;
	private Double slWithE4Per;
	private Double confidence;
	private Double riskPercent;
	private String analysisDetails;

	public StopLossInfoEntity(String strategy, Double stopLoss1) {
		this.strategy = strategy;
		this.stopLoss1 = stopLoss1 != null ? Math.round(stopLoss1 * 100.0) / 100.0 : null;
	}

	public StopLossInfoEntity(String strategy, Double stopLoss1, Double confidence, Double riskPercent) {
		this.strategy = strategy;
		this.stopLoss1 = stopLoss1 != null ? Math.round(stopLoss1 * 100.0) / 100.0 : null;
		this.confidence = confidence;
		this.riskPercent = riskPercent;
	}

	public StopLossInfoEntity(String strategy, Double stopLoss1, Double confidence, Double riskPercent, String analysisDetails) {
		this(strategy, stopLoss1, confidence, riskPercent);
		this.analysisDetails = analysisDetails;
	}
}
