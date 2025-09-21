package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.EntryInfo;
import com.stocks.dto.StopLossInfo;
import com.stocks.dto.StrikeTO;
import com.stocks.dto.TradeSetupTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Advanced Stop Loss Calculation Service
 * Implements multiple sophisticated stop loss strategies based on:
 * - Average True Range (ATR) for volatility-based stops
 * - Support/Resistance levels analysis
 * - Moving averages and trend analysis
 * - Option chain data integration
 * - Risk-reward optimization
 */
@Service
public class AdvancedStopLossService {

	private static final Logger log = LoggerFactory.getLogger(AdvancedStopLossService.class);

	@Value("${stop.loss.atr.multiplier:2.0}")
	private double atrMultiplier;

	@Value("${stop.loss.max.risk.percent:2.0}")
	private double maxRiskPercent;

	@Value("${stop.loss.min.risk.reward.ratio:1.5}")
	private double minRiskRewardRatio;

	/**
	 * Calculate optimal stop loss using multiple strategies and select the best one
	 */
	public void calculateOptimalStopLoss(TradeSetupTO tradeSetup, List<Candle> historicalCandles,
	                                     Map<Integer, StrikeTO> optionChain, String entryStrategy) {

		double entryPrice = tradeSetup.getEntry2();
		String tradeDirection = tradeSetup.getType(); // "P" or "Negative"

		log.info("Calculating optimal stop loss for {} - Entry: {}, Direction: {}",
				tradeSetup.getStockSymbol(), entryPrice, tradeDirection);

		// Calculate stop loss using different methods
		double atrStopLoss = calculateATRBasedStopLoss(historicalCandles, entryPrice, tradeDirection);
		// double supportResistanceStopLoss = calculateSupportResistanceStopLoss(historicalCandles, entryPrice, tradeDirection);
//		double movingAverageStopLoss = calculateMovingAverageStopLoss(historicalCandles, entryPrice, tradeDirection);
//		double optionChainStopLoss = calculateOptionChainBasedStopLoss(optionChain, entryPrice, tradeDirection);
//		double volatilityAdjustedStopLoss = calculateVolatilityAdjustedStopLoss(historicalCandles, entryPrice, tradeDirection);

		// Weight and combine different methods
//		double optimalStopLoss = combineStopLossStrategies(
//				atrStopLoss, supportResistanceStopLoss, movingAverageStopLoss,
//				optionChainStopLoss, volatilityAdjustedStopLoss, entryPrice, tradeDirection);

		// Validate against risk management rules
//		double validatedStopLoss = validateStopLossAgainstRiskRules(optimalStopLoss, entryPrice,
//				tradeSetup.getTarget1(), tradeDirection);
		if (atrStopLoss < entryPrice) {
			StopLossInfo stopLossInfo = new StopLossInfo("ATR", atrStopLoss);
			calculatePercent(tradeSetup, stopLossInfo);
			tradeSetup.getStopLossInfos().add(stopLossInfo);
		}
//		if (supportResistanceStopLoss < entryPrice) {
//			StopLossInfo stopLossInfo1 = new StopLossInfo("S" + "-" + entryStrategy, supportResistanceStopLoss);
//			calculatePercent(tradeSetup, stopLossInfo1);
//			tradeSetup.getStopLossInfos().add(stopLossInfo1);
//		}
		// if (movingAverageStopLoss < entryPrice) {
		// 	StopLossInfo stopLossInfo2 = new StopLossInfo("MA" + "-" + entryStrategy, movingAverageStopLoss);
		// 	calculatePercent(tradeSetup, stopLossInfo2);
		// 	tradeSetup.getStopLossInfos().add(stopLossInfo2);
		// }
//		if (optionChainStopLoss < entryPrice) {
//			StopLossInfo stopLossInfo3 = new StopLossInfo("OC" + "-" + entryStrategy, optionChainStopLoss);
//			calculatePercent(tradeSetup, stopLossInfo3);
//			tradeSetup.getStopLossInfos().add(stopLossInfo3);
//		}
// 		if (volatilityAdjustedStopLoss < entryPrice) {
// 			StopLossInfo stopLossInfo4 = new StopLossInfo("Vol" + "-" + entryStrategy, volatilityAdjustedStopLoss);
// 			calculatePercent(tradeSetup, stopLossInfo4);
// 			tradeSetup.getStopLossInfos().add(stopLossInfo4);
// 		}
// 		if (optimalStopLoss < entryPrice) {
// 			StopLossInfo stopLossInfo5 = new StopLossInfo("Mul" + "-" + entryStrategy, optimalStopLoss);
// 			calculatePercent(tradeSetup, stopLossInfo5);
// 			tradeSetup.getStopLossInfos().add(stopLossInfo5);
// 		}
// //		tradeSetup.getStopLossInfos().add(new StopLossInfo("RI", Math.round(validatedStopLoss * 100.0) / 100.0));
// 		if (tradeSetup.getStopLoss1() != null) {
// 			StopLossInfo stopLossInfo6 = new StopLossInfo("D" + "-" + entryStrategy, tradeSetup.getStopLoss1());
// 			calculatePercent(tradeSetup, stopLossInfo6);
// 			tradeSetup.getStopLossInfos().add(stopLossInfo6);
// 		}

//		log.info("Stop loss calculation for {}: ATR={}, S/R={}, MA={}, OC={}, Vol={}",
//				tradeSetup.getStockSymbol(), atrStopLoss, supportResistanceStopLoss,
//				movingAverageStopLoss, optionChainStopLoss, volatilityAdjustedStopLoss);

	}

	/**
	 * ATR-based stop loss calculation
	 * Uses Average True Range to account for volatility
	 */
	private double calculateATRBasedStopLoss(List<Candle> candles, double entryPrice, String direction) {
		if (candles == null || candles.size() < 14) {
			log.warn("Insufficient data for ATR calculation, using fallback");
			// Closer fallback distance: 2% below entry for long positions
			return "P".equals(direction) ? entryPrice * 0.98 : entryPrice * 1.02;
		}

		double atr = calculateATR(candles, 14);
		// Closer ATR multiplier - tighter stops
		double closerMultiplier = Math.max(atrMultiplier * 1.0, 1.5); // At least 1.5x ATR
		double atrStopDistance = atr * closerMultiplier;

		if ("P".equals(direction)) {
			// Tighter distance range: minimum 1%, maximum 2.5%
			double minStopDistance = entryPrice * 0.01;  // 1% minimum
			double maxStopDistance = entryPrice * 0.025; // 2.5% maximum
			double actualStopDistance = Math.min(Math.max(atrStopDistance, minStopDistance), maxStopDistance);
			return entryPrice - actualStopDistance;
		} else {
			// For short positions
			double minStopDistance = entryPrice * 0.01;  // 1% minimum
			double maxStopDistance = entryPrice * 0.025; // 2.5% maximum
			double actualStopDistance = Math.min(Math.max(atrStopDistance, minStopDistance), maxStopDistance);
			return entryPrice + actualStopDistance;
		}
	}

	private void calculatePercent(TradeSetupTO tradeSetup, StopLossInfo stopLossInfo) {

		for (EntryInfo entry : tradeSetup.getEntryInfos()) {
			double per = ((entry.getEntryPrice() - stopLossInfo.getStopLoss()) / entry.getEntryPrice()) * 100.0;
			if ("BC".equals(entry.getStrategy())) {
				stopLossInfo.setSlWithE1Per(Math.round(per * 100.0) / 100.0);
			} else if ("MR".equals(entry.getStrategy())) {
				stopLossInfo.setSlWithE2Per(Math.round(per * 100.0) / 100.0);
			} else if ("D".equals(entry.getStrategy())) {
				stopLossInfo.setSlWithE3Per(Math.round(per * 100.0) / 100.0);
			}
		}
	}

	/**
	 * Calculate Average True Range
	 */
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

	/**
	 * Support and Resistance based stop loss
	 */
	private double calculateSupportResistanceStopLoss(List<Candle> candles, double entryPrice, String direction) {
		if (candles == null || candles.size() < 20) {
			// Increased fallback distance: 3% below entry for long positions
			return "P".equals(direction) ? entryPrice * 0.97 : entryPrice * 1.03;
		}

		if ("P".equals(direction)) {
			// Find nearest support level below entry price
			double supportLevel = findNearestSupportLevel(candles, entryPrice);
			double supportBasedStop = supportLevel * 0.995; // Further below support

			// Ensure minimum 2% distance from entry price
			double minStop = entryPrice * 0.98;
			return Math.min(supportBasedStop, minStop);
		} else {
			// Find nearest resistance level above entry price
			double resistanceLevel = findNearestResistanceLevel(candles, entryPrice);
			double resistanceBasedStop = resistanceLevel * 1.005; // Further above resistance

			// Ensure minimum 2% distance from entry price
			double minStop = entryPrice * 1.02;
			return Math.max(resistanceBasedStop, minStop);
		}
	}

	/**
	 * Find nearest support level below the given price
	 */
	private double findNearestSupportLevel(List<Candle> candles, double price) {
		double nearestSupport = 0;

		// Look for swing lows in recent data
		for (int i = 2; i < candles.size() - 2; i++) {
			Candle current = candles.get(i);
			Candle prev1 = candles.get(i - 1);
			Candle prev2 = candles.get(i - 2);
			Candle next1 = candles.get(i + 1);
			Candle next2 = candles.get(i + 2);

			// Check if current candle is a swing low
			if (current.getLow() < prev1.getLow() && current.getLow() < prev2.getLow() &&
					current.getLow() < next1.getLow() && current.getLow() < next2.getLow() &&
					current.getLow() < price) {

				if (nearestSupport == 0 || current.getLow() > nearestSupport) {
					nearestSupport = current.getLow();
				}
			}
		}

		return nearestSupport > 0 ? nearestSupport : price * 0.98;
	}

	/**
	 * Find nearest resistance level above the given price
	 */
	private double findNearestResistanceLevel(List<Candle> candles, double price) {
		double nearestResistance = Double.MAX_VALUE;

		// Look for swing highs in recent data
		for (int i = 2; i < candles.size() - 2; i++) {
			Candle current = candles.get(i);
			Candle prev1 = candles.get(i - 1);
			Candle prev2 = candles.get(i - 2);
			Candle next1 = candles.get(i + 1);
			Candle next2 = candles.get(i + 2);

			// Check if current candle is a swing high
			if (current.getHigh() > prev1.getHigh() && current.getHigh() > prev2.getHigh() &&
					current.getHigh() > next1.getHigh() && current.getHigh() > next2.getHigh() &&
					current.getHigh() > price) {

				if (current.getHigh() < nearestResistance) {
					nearestResistance = current.getHigh();
				}
			}
		}

		return nearestResistance != Double.MAX_VALUE ? nearestResistance : price * 1.02;
	}

	/**
	 * Moving average based stop loss
	 */
	private double calculateMovingAverageStopLoss(List<Candle> candles, double entryPrice, String direction) {
		if (candles == null || candles.size() < 20) {
			// Increased fallback distance: 3% below entry for long positions
			return "P".equals(direction) ? entryPrice * 0.97 : entryPrice * 1.03;
		}
		double sma20 = calculateSMA(candles, 20);
		double sma50 = candles.size() >= 50 ? calculateSMA(candles, 50) : sma20;

		if ("P".equals(direction)) {
			// Use the higher of the two moving averages as support
			double maSupport = Math.max(sma20, sma50);
			double maBasedStop = maSupport * 0.99; // Further below MA support

			// Ensure minimum 2.5% distance from entry price
			double minStop = entryPrice * 0.975;
			return Math.min(maBasedStop, minStop);
		} else {
			// Use the lower of the two moving averages as resistance
			double maResistance = Math.min(sma20, sma50);
			double maBasedStop = maResistance * 1.01; // Further above MA resistance

			// Ensure minimum 2.5% distance from entry price
			double minStop = entryPrice * 1.025;
			return Math.max(maBasedStop, minStop);
		}
	}

	/**
	 * Calculate Simple Moving Average
	 */
	private double calculateSMA(List<Candle> candles, int period) {
		if (candles.size() < period) {
			period = candles.size();
		}

		double sum = 0.0;
		for (int i = 0; i < period; i++) {
			sum += candles.get(i).getClose();
		}
		return sum / period;
	}

	/**
	 * Option chain based stop loss using OI and volume data
	 */
	private double calculateOptionChainBasedStopLoss(Map<Integer, StrikeTO> optionChain,
	                                                 double entryPrice, String direction) {
		if (optionChain == null || optionChain.isEmpty()) {
			// Increased fallback distance: 3% below entry for long positions
			return "P".equals(direction) ? entryPrice * 0.97 : entryPrice * 1.03;
		}

		if ("P".equals(direction)) {
			// Find strike with highest PUT OI below entry price
			// Process strikes in descending order: 1350, 1300, 1250, etc.
			int maxPutOI = 0;
			double supportStrike = entryPrice * 0.975; // Increased distance

			// Process strikes below entry price in descending order (closest first)
			optionChain.values().stream()
					.filter(strike -> strike.getStrikePrice() < entryPrice)
					.sorted((s1, s2) -> Double.compare(s2.getStrikePrice(), s1.getStrikePrice()))
					.forEach(strike -> {
						// This will process in order: 1350, 1300, 1250, etc.
						log.debug("Checking PUT strike {} with OI {}", strike.getStrikePrice(), strike.getPeOi());
					});

			// Find the strike with highest PUT OI among strikes below entry price
			for (StrikeTO strike : optionChain.values()) {
				if (strike.getStrikePrice() < entryPrice && strike.getPeOi() > maxPutOI) {
					maxPutOI = strike.getPeOi();
					supportStrike = strike.getStrikePrice();
				}
			}
			// Ensure minimum 2% distance from entry and further below support strike
			double ocBasedStop = supportStrike * 0.99;
			double minStop = entryPrice * 0.98;
			return Math.min(ocBasedStop, minStop);
		} else {
			// Find strike with highest CALL OI above entry price  
			// Process strikes in ascending order: 1400, 1450, 1500, etc.
			int maxCallOI = 0;
			double resistanceStrike = entryPrice * 1.02;

			// Process strikes above entry price in ascending order (closest first)
			optionChain.values().stream()
					.filter(strike -> strike.getStrikePrice() > entryPrice)
					.sorted((s1, s2) -> Double.compare(s1.getStrikePrice(), s2.getStrikePrice()))
					.forEach(strike -> {
						// This will process in order: 1400, 1450, 1500, etc.
						log.debug("Checking CALL strike {} with OI {}", strike.getStrikePrice(), strike.getCeOi());
					});

			// Find the strike with highest CALL OI among strikes above entry price
			for (StrikeTO strike : optionChain.values()) {
				if (strike.getStrikePrice() > entryPrice && strike.getCeOi() > maxCallOI) {
					maxCallOI = strike.getCeOi();
					resistanceStrike = strike.getStrikePrice();
				}
			}
			// Ensure minimum 2% distance from entry and further above resistance strike
			double ocBasedStop = resistanceStrike * 1.01;
			double minStop = entryPrice * 1.02;
			return Math.max(ocBasedStop, minStop);
		}
	}

	/**
	 * Volatility adjusted stop loss using recent price volatility
	 */
	private double calculateVolatilityAdjustedStopLoss(List<Candle> candles, double entryPrice, String direction) {
		if (candles == null || candles.size() < 10) {
			// Increased fallback distance: 3% below entry for long positions
			return "P".equals(direction) ? entryPrice * 0.97 : entryPrice * 1.03;
		}

		// Calculate recent volatility (standard deviation of returns)
		double volatility = calculateVolatility(candles, 10);
		// Increased volatility multiplier for wider stops
		double volatilityAdjustment = Math.max(volatility * 2.5, 0.025); // At least 2.5% distance

		if ("P".equals(direction)) {
			double volBasedStop = entryPrice * (1 - volatilityAdjustment);
			// Ensure minimum 2.5% distance from entry
			double minStop = entryPrice * 0.975;
			return Math.min(volBasedStop, minStop);
		} else {
			double volBasedStop = entryPrice * (1 + volatilityAdjustment);
			// Ensure minimum 2.5% distance from entry
			double minStop = entryPrice * 1.025;
			return Math.max(volBasedStop, minStop);
		}
	}

	/**
	 * Calculate price volatility (standard deviation of returns)
	 */
	private double calculateVolatility(List<Candle> candles, int period) {
		if (candles.size() < period + 1) {
			period = candles.size() - 1;
		}

		// Calculate returns
		double[] returns = new double[period];
		for (int i = 1; i <= period; i++) {
			returns[i - 1] = (candles.get(i).getClose() - candles.get(i - 1).getClose()) / candles.get(i - 1).getClose();
		}

		// Calculate mean return
		double meanReturn = 0;
		for (double ret : returns) {
			meanReturn += ret;
		}
		meanReturn /= returns.length;

		// Calculate standard deviation
		double variance = 0;
		for (double ret : returns) {
			variance += Math.pow(ret - meanReturn, 2);
		}
		variance /= returns.length;

		return Math.sqrt(variance);
	}

	/**
	 * Combine multiple stop loss strategies using weighted average
	 */
	private double combineStopLossStrategies(double atrStopLoss, double supportResistanceStopLoss,
	                                         double movingAverageStopLoss, double optionChainStopLoss,
	                                         double volatilityAdjustedStopLoss, double entryPrice, String direction) {

		// Weights for different strategies (can be made configurable)
		double atrWeight = 0.3;
		double srWeight = 0.25;
		double maWeight = 0.2;
		double ocWeight = 0.15;
		double volWeight = 0.1;

		double weightedStopLoss = (atrStopLoss * atrWeight) +
				(supportResistanceStopLoss * srWeight) +
				(movingAverageStopLoss * maWeight) +
				(optionChainStopLoss * ocWeight) +
				(volatilityAdjustedStopLoss * volWeight);

		return weightedStopLoss;
	}

	/**
	 * Validate stop loss against risk management rules
	 */
	private double validateStopLossAgainstRiskRules(double stopLoss, double entryPrice,
	                                                double targetPrice, String direction) {

		// Calculate risk and reward
		double risk = Math.abs(entryPrice - stopLoss);
		double reward = Math.abs(targetPrice - entryPrice);
		double riskRewardRatio = reward / risk;

		// Ensure minimum risk-reward ratio
		if (riskRewardRatio < minRiskRewardRatio) {
			log.warn("Risk-reward ratio {} is below minimum {}, adjusting stop loss",
					riskRewardRatio, minRiskRewardRatio);

			double adjustedRisk = reward / minRiskRewardRatio;
			if ("P".equals(direction)) {
				stopLoss = entryPrice - adjustedRisk;
			} else {
				stopLoss = entryPrice + adjustedRisk;
			}
		}

		// Ensure maximum risk percentage
		double riskPercent = (risk / entryPrice) * 100;
		if (riskPercent > maxRiskPercent) {
			log.warn("Risk percentage {} exceeds maximum {}, adjusting stop loss",
					riskPercent, maxRiskPercent);

			double maxRisk = entryPrice * (maxRiskPercent / 100);
			if ("P".equals(direction)) {
				stopLoss = entryPrice - maxRisk;
			} else {
				stopLoss = entryPrice + maxRisk;
			}
		}

		return Math.round(stopLoss * 100.0) / 100.0; // Round to 2 decimal places
	}

}
