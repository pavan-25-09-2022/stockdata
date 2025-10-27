package com.stocks.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EntryInfo {
	private String strategy;
	private double entryPrice;
	private String entryTime;
	private String entrySignal; // BUY, SELL, HOLD
	private double confidence;
	private double riskPercent;

	// Individual strategy scores
	private double breakoutScore;
	private double meanReversionScore;
	private double volumePriceScore;

	private String analysisDetails;

	public EntryInfo(String strategy, double entryPrice) {
		this.strategy = strategy;
		this.entryPrice = Math.round(entryPrice * 100.0) / 100.0;
	}

	public EntryInfo(String strategy, double entryPrice, String entrySignal,
	                 double confidence, double riskPercent, double breakoutScore,
	                 double meanReversionScore, double volumePriceScore) {
		this.strategy = strategy;
		this.entryPrice = Math.round(entryPrice * 100.0) / 100.0;
		this.entrySignal = entrySignal;
		this.confidence = Math.round(confidence * 100.0) / 100.0;
		this.riskPercent = Math.round(riskPercent * 100.0) / 100.0;
		this.breakoutScore = Math.round(breakoutScore * 100.0) / 100.0;
		this.meanReversionScore = Math.round(meanReversionScore * 100.0) / 100.0;
		this.volumePriceScore = Math.round(volumePriceScore * 100.0) / 100.0;
	}

	public EntryInfo(String strategy, double entryPrice, String entrySignal,
	                 double confidence, double riskPercent, double breakoutScore,
	                 double meanReversionScore, double volumePriceScore, String analysisDetails) {
		this(strategy, entryPrice, entrySignal, confidence, riskPercent,
				breakoutScore, meanReversionScore, volumePriceScore);
		this.analysisDetails = analysisDetails;
	}
}
