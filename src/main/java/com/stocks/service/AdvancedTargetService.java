package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.EntryInfo;
import com.stocks.dto.StrikeTO;
import com.stocks.dto.TargetInfo;
import com.stocks.dto.TradeSetupTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Advanced Target Calculation Service
 * Implements multiple sophisticated target strategies based on:
 * - Fibonacci retracement and extension levels
 * - Support/Resistance projection analysis
 * - ATR-based target calculation
 * - Option chain max pain and high OI analysis
 * - Volume profile and VWAP targets
 * - Risk-reward optimization
 */
@Service
public class AdvancedTargetService {

	private static final Logger log = LoggerFactory.getLogger(AdvancedTargetService.class);

	@Value("${target.fibonacci.extension.ratio1:1.618}")
	private double fibExtensionRatio1;

	@Value("${target.fibonacci.extension.ratio2:2.618}")
	private double fibExtensionRatio2;

	@Value("${target.atr.multiplier:3.0}")
	private double atrMultiplier;

	@Value("${target.min.risk.reward.ratio:2.0}")
	private double minRiskRewardRatio;

	@Value("${target.max.risk.reward.ratio:5.0}")
	private double maxRiskRewardRatio;

	@Value("${target.volume.threshold:1.5}")
	private double volumeThreshold;

	/**
	 * Calculate optimal target prices using multiple strategies
	 */
	public void calculateOptimalTargets(TradeSetupTO tradeSetup, List<Candle> historicalCandles,
	                                    Map<Integer, StrikeTO> optionChain, String entryStrategy) {

		double entryPrice = tradeSetup.getEntry1();
		Double stopLoss = tradeSetup.getStopLoss1();
		String tradeDirection = tradeSetup.getType(); // "P" or "Negative"

		log.info("Calculating optimal targets for {} - Entry: {}, StopLoss: {}, Direction: {}",
				tradeSetup.getStockSymbol(), entryPrice, stopLoss, tradeDirection);

		// Calculate targets using different methods
		TargetInfo fibTargets = calculateFibonacciTargets(historicalCandles, entryPrice, stopLoss, tradeDirection);
//		TargetInfo srTargets = calculateSupportResistanceTargets(historicalCandles, entryPrice, tradeDirection);
//		TargetInfo atrTargets = calculateATRBasedTargets(historicalCandles, entryPrice, stopLoss, tradeDirection);
//		TargetInfo optionTargets = calculateOptionChainTargets(optionChain, entryPrice, tradeDirection);
//		TargetInfo vwapTargets = calculateVWAPTargets(historicalCandles, entryPrice, tradeDirection);
		if (fibTargets.getTarget1() > entryPrice && fibTargets.getTarget2() > entryPrice) {
			calculatePercent(tradeSetup, fibTargets);
			fibTargets.setStrategy(fibTargets.getStrategy());
			tradeSetup.getTargetInfos().add(fibTargets);
		}
//		if (srTargets.getTarget1() > entryPrice && srTargets.getTarget2() > entryPrice) {
//			calculatePercent(tradeSetup, srTargets);
//			srTargets.setStrategy(srTargets.getStrategy() + "-" + entryStrategy);
//			tradeSetup.getTargetInfos().add(srTargets);
//		}
		// if (atrTargets.getTarget1() > entryPrice && atrTargets.getTarget2() > entryPrice) {
		// 	calculatePercent(tradeSetup, atrTargets);
		// 	atrTargets.setStrategy(atrTargets.getStrategy() + "-" + entryStrategy);
		// 	tradeSetup.getTargetInfos().add(atrTargets);
		// }
		// if (optionTargets.getTarget1() > entryPrice && optionTargets.getTarget2() > entryPrice) {
		// 	calculatePercent(tradeSetup, optionTargets);
		// 	optionTargets.setStrategy(optionTargets.getStrategy() + "-" + entryStrategy);
		// 	tradeSetup.getTargetInfos().add(optionTargets);
		// }
		// if (vwapTargets.getTarget1() > entryPrice && vwapTargets.getTarget2() > entryPrice) {
		// 	calculatePercent(tradeSetup, vwapTargets);
		// 	vwapTargets.setStrategy(vwapTargets.getStrategy() + "-" + entryStrategy);
		// 	tradeSetup.getTargetInfos().add(vwapTargets);
		// }
		if (tradeSetup.getTarget1() != null && tradeSetup.getTarget2() != null) {
			TargetInfo target = new TargetInfo("C2", tradeSetup.getTarget1(), tradeSetup.getTarget2());
			if (target.getTarget1() > entryPrice && target.getTarget2() > entryPrice) {
				calculatePercent(tradeSetup, target);
				target.setStrategy(target.getStrategy());
				tradeSetup.getTargetInfos().add(target);
			}
		}

		// Combine and optimize targets
//		TargetInfo optimalTargets = combineTargetStrategies(
//				fibTargets, srTargets, atrTargets, optionTargets, vwapTargets);
//		calculatePercent(tradeSetup, optimalTargets);
//		optimalTargets.setStrategy(optimalTargets.getStrategy() + "-" + entryStrategy);
//		tradeSetup.getTargetInfos().add(optimalTargets);

	}

	private void calculatePercent(TradeSetupTO tradeSetup, TargetInfo targetInfo) {

		for (EntryInfo entry : tradeSetup.getEntryInfos()) {
			double per = ((targetInfo.getTarget1() - entry.getEntryPrice()) / entry.getEntryPrice()) * 100.0;
			double per1 = ((targetInfo.getTarget2() - entry.getEntryPrice()) / entry.getEntryPrice()) * 100.0;

			if ("BC".equals(entry.getStrategy())) {
				targetInfo.setT1WithE1Per(Math.round(per * 100.0) / 100.0);
				targetInfo.setT2WithE1Per(Math.round(per1 * 100.0) / 100.0);
			} else if ("MR".equals(entry.getStrategy())) {
				targetInfo.setT1WithE2Per(Math.round(per * 100.0) / 100.0);
				targetInfo.setT2WithE2Per(Math.round(per1 * 100.0) / 100.0);
			} else if ("D".equals(entry.getStrategy())) {
				targetInfo.setT1WithE3Per(Math.round(per * 100.0) / 100.0);
				targetInfo.setT2WithE3Per(Math.round(per1 * 100.0) / 100.0);
			}
		}
	}

	/**
	 * Fibonacci-based target calculation
	 * Uses Fibonacci extensions and retracements for target projection
	 */
	private TargetInfo calculateFibonacciTargets(List<Candle> candles, double entryPrice,
	                                             double stopLoss, String direction) {

		// Find recent swing high and low for Fibonacci calculation
		SwingPoints swingPoints = findRecentSwingPoints(candles);

		double swingRange = Math.abs(swingPoints.swingHigh - swingPoints.swingLow);

		if ("P".equals(direction)) {
			// For long trades, project upward from entry using Fibonacci extensions
			double target1 = entryPrice + (swingRange * fibExtensionRatio1);
			double target2 = entryPrice + (swingRange * fibExtensionRatio2);
			return new TargetInfo("FE", target1, target2);
		} else {
			// For short trades, project downward from entry
			double target1 = entryPrice - (swingRange * fibExtensionRatio1);
			double target2 = entryPrice - (swingRange * fibExtensionRatio2);
			return new TargetInfo("FE", target1, target2);
		}
	}

	/**
	 * Support/Resistance based target calculation
	 * Projects targets to next significant S/R levels
	 */
	private TargetInfo calculateSupportResistanceTargets(List<Candle> candles, double entryPrice, String direction) {

		if ("P".equals(direction)) {
			// Find resistance levels above entry price
			double[] resistanceLevels = findResistanceLevels(candles, entryPrice);
			return new TargetInfo("R", resistanceLevels[0], resistanceLevels[1]);
		} else {
			// Find support levels below entry price
			double[] supportLevels = findSupportLevels(candles, entryPrice);
			return new TargetInfo("S", supportLevels[0], supportLevels[1]);
		}
	}

	/**
	 * ATR-based target calculation
	 * Uses Average True Range for volatility-adjusted targets
	 */
	private TargetInfo calculateATRBasedTargets(List<Candle> candles, double entryPrice,
	                                            double stopLoss, String direction) {

		double atr = calculateATR(candles, 14);
		double riskAmount = Math.abs(entryPrice - stopLoss);

		if ("P".equals(direction)) {
			double target1 = entryPrice + (atr * atrMultiplier);
			double target2 = entryPrice + (riskAmount * 3.0); // 3:1 risk-reward
			return new TargetInfo("ATR", target1, target2);
		} else {
			double target1 = entryPrice - (atr * atrMultiplier);
			double target2 = entryPrice - (riskAmount * 3.0); // 3:1 risk-reward
			return new TargetInfo("ATR", target1, target2);
		}
	}

	/**
	 * Option chain based target calculation
	 * Uses high OI strikes and max pain levels as targets
	 */
	private TargetInfo calculateOptionChainTargets(Map<Integer, StrikeTO> optionChain,
	                                               double entryPrice, String direction) {

		if ("P".equals(direction)) {
			// Find strikes with high CALL OI above entry price
			double[] callTargets = findHighCallOIStrikes(optionChain, entryPrice);
			return new TargetInfo("OC", callTargets[0], callTargets[1]);
		} else {
			// Find strikes with high PUT OI below entry price
			double[] putTargets = findHighPutOIStrikes(optionChain, entryPrice);
			return new TargetInfo("OC", putTargets[0], putTargets[1]);
		}
	}

	/**
	 * VWAP-based target calculation
	 * Uses Volume Weighted Average Price and volume profile
	 */
	private TargetInfo calculateVWAPTargets(List<Candle> candles, double entryPrice, String direction) {

		double vwap = calculateVWAP(candles);
		double[] volumeNodes = findHighVolumeNodes(candles, entryPrice, direction);

		if ("P".equals(direction)) {
			double target1 = Math.max(vwap * 1.02, volumeNodes[0]);
			double target2 = volumeNodes[1];
			return new TargetInfo("VWAP", target1, target2);
		} else {
			double target1 = Math.min(vwap * 0.98, volumeNodes[0]);
			double target2 = volumeNodes[1];
			return new TargetInfo("VWAP", target1, target2);
		}
	}

	/**
	 * Combine multiple target strategies using weighted approach
	 */
	private TargetInfo combineTargetStrategies(TargetInfo fibTargets, TargetInfo srTargets,
	                                           TargetInfo atrTargets, TargetInfo optionTargets,
	                                           TargetInfo vwapTargets) {

		// Weights for different strategies (can be made configurable)
		double fibWeight = 0.25;
		double srWeight = 0.25;
		double atrWeight = 0.20;
		double optionWeight = 0.20;
		double vwapWeight = 0.10;

		double target1 = (fibTargets.getTarget1() * fibWeight) +
				(srTargets.getTarget1() * srWeight) +
				(atrTargets.getTarget1() * atrWeight) +
				(optionTargets.getTarget1() * optionWeight) +
				(vwapTargets.getTarget1() * vwapWeight);

		double target2 = (fibTargets.getTarget2() * fibWeight) +
				(srTargets.getTarget2() * srWeight) +
				(atrTargets.getTarget2() * atrWeight) +
				(optionTargets.getTarget2() * optionWeight) +
				(vwapTargets.getTarget2() * vwapWeight);

		return new TargetInfo("MUl", target1, target2);
	}

//	/**
//	 * Validate targets against risk management rules
//	 */
//	private TargetInfo validateTargetsAgainstRiskRules(TargetInfo targets, double entryPrice,
//	                                                   double stopLoss, String direction) {
//
//		double riskAmount = Math.abs(entryPrice - stopLoss);
//		double target1 = targets.target1;
//		double target2 = targets.target2;
//
//		// Calculate risk-reward ratios
//		double reward1 = Math.abs(target1 - entryPrice);
//		double reward2 = Math.abs(target2 - entryPrice);
//		double riskReward1 = reward1 / riskAmount;
//		double riskReward2 = reward2 / riskAmount;
//
//		// Ensure minimum risk-reward ratio for target1
//		if (riskReward1 < minRiskRewardRatio) {
//			log.warn("Target1 risk-reward ratio {} is below minimum {}, adjusting",
//					riskReward1, minRiskRewardRatio);
//
//			if ("P".equals(direction)) {
//				target1 = entryPrice + (riskAmount * minRiskRewardRatio);
//			} else {
//				target1 = entryPrice - (riskAmount * minRiskRewardRatio);
//			}
//		}
//
//		// Ensure minimum risk-reward ratio for target2
//		if (riskReward2 < minRiskRewardRatio * 1.5) { // Target2 should be 1.5x better than target1
//			log.warn("Target2 risk-reward ratio {} is below minimum {}, adjusting",
//					riskReward2, minRiskRewardRatio * 1.5);
//
//			if ("P".equals(direction)) {
//				target2 = entryPrice + (riskAmount * minRiskRewardRatio * 1.5);
//			} else {
//				target2 = entryPrice - (riskAmount * minRiskRewardRatio * 1.5);
//			}
//		}
//
//		// Cap maximum risk-reward ratio to prevent unrealistic targets
//		if (riskReward1 > maxRiskRewardRatio) {
//			if ("P".equals(direction)) {
//				target1 = entryPrice + (riskAmount * maxRiskRewardRatio);
//			} else {
//				target1 = entryPrice - (riskAmount * maxRiskRewardRatio);
//			}
//		}
//
//		if (riskReward2 > maxRiskRewardRatio * 1.5) {
//			if ("P".equals(direction)) {
//				target2 = entryPrice + (riskAmount * maxRiskRewardRatio * 1.5);
//			} else {
//				target2 = entryPrice - (riskAmount * maxRiskRewardRatio * 1.5);
//			}
//		}
//
//		// Ensure target2 is further than target1
//		if ("P".equals(direction)) {
//			if (target2 <= target1) {
//				target2 = target1 + (riskAmount * 0.5);
//			}
//		} else {
//			if (target2 >= target1) {
//				target2 = target1 - (riskAmount * 0.5);
//			}
//		}
//
//		return new TargetInfo("RB", Math.round(target1 * 100.0) / 100.0, Math.round(target2 * 100.0) / 100.0
//		);
//	}


	private SwingPoints findRecentSwingPoints(List<Candle> candles) {
		double swingHigh = 0;
		double swingLow = Double.MAX_VALUE;

		// Look for swing points in recent 20 candles
		int lookback = Math.min(20, candles.size());
		for (int i = 0; i < lookback; i++) {
			Candle candle = candles.get(i);
			if (candle.getHigh() > swingHigh) {
				swingHigh = candle.getHigh();
			}
			if (candle.getLow() < swingLow) {
				swingLow = candle.getLow();
			}
		}

		return new SwingPoints(swingHigh, swingLow == Double.MAX_VALUE ? 0 : swingLow);
	}

	private double[] findResistanceLevels(List<Candle> candles, double entryPrice) {
		double[] resistanceLevels = new double[2];
		double nearestResistance = Double.MAX_VALUE;
		double secondResistance = Double.MAX_VALUE;

		// Find resistance levels above entry price
		for (int i = 2; i < candles.size() - 2; i++) {
			Candle current = candles.get(i);
			Candle prev1 = candles.get(i - 1);
			Candle prev2 = candles.get(i - 2);
			Candle next1 = candles.get(i + 1);
			Candle next2 = candles.get(i + 2);

			// Check if current candle is a swing high
			if (current.getHigh() > prev1.getHigh() && current.getHigh() > prev2.getHigh() &&
					current.getHigh() > next1.getHigh() && current.getHigh() > next2.getHigh() &&
					current.getHigh() > entryPrice) {

				if (current.getHigh() < nearestResistance) {
					secondResistance = nearestResistance;
					nearestResistance = current.getHigh();
				} else if (current.getHigh() < secondResistance) {
					secondResistance = current.getHigh();
				}
			}
		}

		resistanceLevels[0] = nearestResistance != Double.MAX_VALUE ? nearestResistance : entryPrice * 1.03;
		resistanceLevels[1] = secondResistance != Double.MAX_VALUE ? secondResistance : entryPrice * 1.05;

		return resistanceLevels;
	}

	private double[] findSupportLevels(List<Candle> candles, double entryPrice) {
		double[] supportLevels = new double[2];
		double nearestSupport = 0;
		double secondSupport = 0;

		// Find support levels below entry price
		for (int i = 2; i < candles.size() - 2; i++) {
			Candle current = candles.get(i);
			Candle prev1 = candles.get(i - 1);
			Candle prev2 = candles.get(i - 2);
			Candle next1 = candles.get(i + 1);
			Candle next2 = candles.get(i + 2);

			// Check if current candle is a swing low
			if (current.getLow() < prev1.getLow() && current.getLow() < prev2.getLow() &&
					current.getLow() < next1.getLow() && current.getLow() < next2.getLow() &&
					current.getLow() < entryPrice) {

				if (current.getLow() > nearestSupport) {
					secondSupport = nearestSupport;
					nearestSupport = current.getLow();
				} else if (current.getLow() > secondSupport) {
					secondSupport = current.getLow();
				}
			}
		}

		supportLevels[0] = nearestSupport > 0 ? nearestSupport : entryPrice * 0.97;
		supportLevels[1] = secondSupport > 0 ? secondSupport : entryPrice * 0.95;

		return supportLevels;
	}

	private double[] findHighCallOIStrikes(Map<Integer, StrikeTO> optionChain, double entryPrice) {
		double[] callTargets = {entryPrice * 1.03, entryPrice * 1.05};
		double maxCallOI1 = 0;
		double maxCallOI2 = 0;

		for (StrikeTO strike : optionChain.values()) {
			if (strike.getStrikePrice() > entryPrice && strike.getCeOi() > 0) {
				if (strike.getCeOi() > maxCallOI1) {
					maxCallOI2 = maxCallOI1;
					callTargets[1] = callTargets[0];
					maxCallOI1 = strike.getCeOi();
					callTargets[0] = strike.getStrikePrice();
				} else if (strike.getCeOi() > maxCallOI2) {
					maxCallOI2 = strike.getCeOi();
					callTargets[1] = strike.getStrikePrice();
				}
			}
		}

		return callTargets;
	}

	private double[] findHighPutOIStrikes(Map<Integer, StrikeTO> optionChain, double entryPrice) {
		double[] putTargets = {entryPrice * 0.97, entryPrice * 0.95};
		double maxPutOI1 = 0;
		double maxPutOI2 = 0;

		for (StrikeTO strike : optionChain.values()) {
			if (strike.getStrikePrice() < entryPrice && strike.getPeOi() > 0) {
				if (strike.getPeOi() > maxPutOI1) {
					maxPutOI2 = maxPutOI1;
					putTargets[1] = putTargets[0];
					maxPutOI1 = strike.getPeOi();
					putTargets[0] = strike.getStrikePrice();
				} else if (strike.getPeOi() > maxPutOI2) {
					maxPutOI2 = strike.getPeOi();
					putTargets[1] = strike.getStrikePrice();
				}
			}
		}

		return putTargets;
	}

	private double calculateATR(List<Candle> candles, int period) {
		if (candles.size() < period) {
			period = candles.size();
		}

		double sum = 0.0;
		for (int i = 1; i < period; i++) {
			Candle current = candles.get(i);
			Candle previous = candles.get(i - 1);

			double tr1 = current.getHigh() - current.getLow();
			double tr2 = Math.abs(current.getHigh() - previous.getClose());
			double tr3 = Math.abs(current.getLow() - previous.getClose());

			double trueRange = Math.max(tr1, Math.max(tr2, tr3));
			sum += trueRange;
		}

		return sum / (period - 1);
	}

	private double calculateVWAP(List<Candle> candles) {
		double totalVolumePrice = 0;
		double totalVolume = 0;

		for (Candle candle : candles) {
			double typicalPrice = (candle.getHigh() + candle.getLow() + candle.getClose()) / 3.0;
			double volume = candle.getVolume();

			totalVolumePrice += typicalPrice * volume;
			totalVolume += volume;
		}

		return totalVolume > 0 ? totalVolumePrice / totalVolume : candles.get(0).getClose();
	}

	private double[] findHighVolumeNodes(List<Candle> candles, double entryPrice, String direction) {
		// Simplified volume profile - find candles with above-average volume
		double avgVolume = candles.stream().mapToDouble(Candle::getVolume).average().orElse(0);
		double[] volumeNodes = new double[2];

		if ("P".equals(direction)) {
			volumeNodes[0] = entryPrice * 1.02;
			volumeNodes[1] = entryPrice * 1.04;

			for (Candle candle : candles) {
				if (candle.getVolume() > avgVolume * volumeThreshold && candle.getClose() > entryPrice) {
					if (volumeNodes[0] == entryPrice * 1.02 || candle.getClose() < volumeNodes[0]) {
						volumeNodes[1] = volumeNodes[0];
						volumeNodes[0] = candle.getClose();
					} else if (volumeNodes[1] == entryPrice * 1.04 || candle.getClose() < volumeNodes[1]) {
						volumeNodes[1] = candle.getClose();
					}
				}
			}
		} else {
			volumeNodes[0] = entryPrice * 0.98;
			volumeNodes[1] = entryPrice * 0.96;

			for (Candle candle : candles) {
				if (candle.getVolume() > avgVolume * volumeThreshold && candle.getClose() < entryPrice) {
					if (volumeNodes[0] == entryPrice * 0.98 || candle.getClose() > volumeNodes[0]) {
						volumeNodes[1] = volumeNodes[0];
						volumeNodes[0] = candle.getClose();
					} else if (volumeNodes[1] == entryPrice * 0.96 || candle.getClose() > volumeNodes[1]) {
						volumeNodes[1] = candle.getClose();
					}
				}
			}
		}

		return volumeNodes;
	}

//	/**
//	 * Get target calculation analysis for monitoring
//	 */
//	public String getTargetAnalysis(TradeSetupTO tradeSetup, List<Candle> historicalCandles,
//	                                Map<Integer, StrikeTO> optionChain) {
//
//		double entryPrice = tradeSetup.getEntry1();
//		double stopLoss = tradeSetup.getStopLoss1();
//		String direction = tradeSetup.getType();
//
//		TargetInfo fibTargets = calculateFibonacciTargets(historicalCandles, entryPrice, stopLoss, direction);
//		TargetInfo srTargets = calculateSupportResistanceTargets(historicalCandles, entryPrice, direction);
//		TargetInfo atrTargets = calculateATRBasedTargets(historicalCandles, entryPrice, stopLoss, direction);
//		TargetInfo optionTargets = calculateOptionChainTargets(optionChain, entryPrice, direction);
//		TargetInfo vwapTargets = calculateVWAPTargets(historicalCandles, entryPrice, direction);
//
//		double riskAmount = Math.abs(entryPrice - stopLoss);
//		double reward1 = Math.abs(tradeSetup.getTarget1() - entryPrice);
//		double reward2 = Math.abs(tradeSetup.getTarget2() - entryPrice);
//
//		return String.format(
//				"Target Analysis for %s:\n" +
//						"Entry Price: %.2f, Stop Loss: %.2f, Direction: %s\n" +
//						"Fibonacci: [%.2f, %.2f]\n" +
//						"Support/Resistance: [%.2f, %.2f]\n" +
//						"ATR-based: [%.2f, %.2f]\n" +
//						"Option Chain: [%.2f, %.2f]\n" +
//						"VWAP/Volume: [%.2f, %.2f]\n" +
//						"Final Targets: [%.2f, %.2f]\n" +
//						"Risk Amount: %.2f\n" +
//						"Risk-Reward Ratios: [%.2f:1, %.2f:1]",
//				tradeSetup.getStockSymbol(), entryPrice, stopLoss, direction,
//				fibTargets.target1, fibTargets.target2,
//				srTargets.target1, srTargets.target2,
//				atrTargets.target1, atrTargets.target2,
//				optionTargets.target1, optionTargets.target2,
//				vwapTargets.target1, vwapTargets.target2,
//				tradeSetup.getTarget1(), tradeSetup.getTarget2(),
//				riskAmount,
//				reward1 / riskAmount, reward2 / riskAmount
//		);
//	}

	private static class SwingPoints {
		public final double swingHigh;
		public final double swingLow;

		public SwingPoints(double swingHigh, double swingLow) {
			this.swingHigh = swingHigh;
			this.swingLow = swingLow;
		}
	}
}
