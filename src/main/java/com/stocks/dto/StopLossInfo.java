package com.stocks.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StopLossInfo {

	private String strategy;
	private double stopLoss;
	private String stopLossTime;
	private int stopLossReachedAfter;
	private double slWithE1Per;
	private double slWithE2Per;
	private double slWithE3Per;
	private double confidence;
	private double riskPercent;
	private String analysisDetails;

	public StopLossInfo(String strategy, double stopLoss) {
		this.strategy = strategy;
		this.stopLoss = Math.round(stopLoss * 100.0) / 100.0;
	}

	public StopLossInfo(String strategy, double stopLoss1, double confidence, double riskPercent) {
		this.strategy = strategy;
		this.stopLoss = Math.round(stopLoss1 * 100.0) / 100.0;
		this.confidence = Math.round(confidence * 100.0) / 100.0;
		this.riskPercent = Math.round(riskPercent * 100.0) / 100.0;
	}

	public StopLossInfo(String strategy, double stopLoss1, double confidence, double riskPercent, String analysisDetails) {
		this(strategy, stopLoss1, confidence, riskPercent);
		this.analysisDetails = analysisDetails;
	}
}
