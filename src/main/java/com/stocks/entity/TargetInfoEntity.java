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
@Table(name = "target_info")
@Data
@NoArgsConstructor
public class TargetInfoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trade_setup_id")
	private TradeSetupEntity tradeSetup;

	private String strategy;
	private Double target1;
	private Double target2;
	private String target1Time;
	private String target2Time;
	private Integer target1ReachedAfter;
	private Integer target2ReachedAfter;
	private double t1WithE1Per;
	private double t1WithE2Per;
	private double t1WithE3Per;
	private double t1WithE4Per;
	private double t2WithE1Per;
	private double t2WithE2Per;
	private double t2WithE3Per;
	private double t2WithE4Per;

	public TargetInfoEntity(String strategy, Double target1, Double target2) {
		this.strategy = strategy;
		this.target1 = target1 != null ? Math.round(target1 * 100.0) / 100.0 : null;
		this.target2 = target2 != null ? Math.round(target2 * 100.0) / 100.0 : null;
	}

	public TargetInfoEntity(String strategy, Double target1, Double target2, Double t1WithE1Per, Double t1WithE2Per, Double t1WithE3Per, Double t1WithE4Per, Double t2WithE1Per, Double t2WithE2Per, Double t2WithE3Per, Double t2WithE4Per) {
		this.strategy = strategy;
		this.target1 = target1 != null ? Math.round(target1 * 100.0) / 100.0 : null;
		this.target2 = target2 != null ? Math.round(target2 * 100.0) / 100.0 : null;
		this.t1WithE1Per = t1WithE1Per != null ? t1WithE1Per : 0.0;
		this.t1WithE2Per = t1WithE2Per != null ? t1WithE2Per : 0.0;
		this.t1WithE3Per = t1WithE3Per != null ? t1WithE3Per : 0.0;
		this.t1WithE4Per = t1WithE4Per != null ? t1WithE4Per : 0.0;
		this.t2WithE1Per = t2WithE1Per != null ? t2WithE1Per : 0.0;
		this.t2WithE2Per = t2WithE2Per != null ? t2WithE2Per : 0.0;
		this.t2WithE3Per = t2WithE3Per != null ? t2WithE3Per : 0.0;
		this.t2WithE4Per = t2WithE4Per != null ? t2WithE4Per : 0.0;
	}

}
