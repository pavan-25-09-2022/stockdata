package com.stocks.dto;

import lombok.Data;

@Data
public class TargetInfo {
	private String strategy;
	private double target1;
	private double target2;
	private String target1Time;
	private String target2Time;
	private double t1WithE1Per;
	private double t1WithE2Per;
	private double t1WithE3Per;
	private double t2WithE1Per;
	private double t2WithE2Per;
	private double t2WithE3Per;
	private int target1ReachedAfter;
	private int target2ReachedAfter;

	public TargetInfo(String strategy, double target1, double target2) {
		this.strategy = strategy;
		double value1 = Math.round(target1 * 100.0) / 100.0;
		double value2 = Math.round(target2 * 100.0) / 100.0;
		if (value2 > value1) {
			this.target1 = value1;
			this.target2 = value2;
		} else {
			this.target1 = value2;
			this.target2 = value1;
		}
	}
}
