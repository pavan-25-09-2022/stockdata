package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.EntryInfo;
import com.stocks.dto.StrikeTO;
import com.stocks.dto.TradeSetupTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Advanced Entry Service implementing 3 sophisticated entry strategies:
 * 1. Breakout Confirmation Strategy (40% weight) - Momentum-based entries
 * 2. Mean Reversion Strategy (35% weight) - Counter-trend entries at key levels
 * 3. Volume-Price Analysis Strategy (25% weight) - Institutional flow-based entries
 * <p>
 * Each strategy provides entry signals with confidence scores and risk assessment.
 */
@Service
public class AdvancedEntryService {

	private static final Logger log = LoggerFactory.getLogger(AdvancedEntryService.class);

	// Configuration parameters
	@Value("${entry.breakout.volume.multiplier:1.5}")
	private double breakoutVolumeMultiplier;

	@Value("${entry.breakout.price.buffer:0.5}")
	private double breakoutPriceBuffer;

	@Value("${entry.mean.reversion.rsi.oversold:30}")
	private double meanReversionRSIOversold;

	@Value("${entry.mean.reversion.rsi.overbought:70}")
	private double meanReversionRSIOverbought;

	@Value("${entry.volume.analysis.threshold:1.2}")
	private double volumeAnalysisThreshold;

	@Value("${entry.confidence.threshold:65}")
	private double confidenceThreshold;

	@Value("${entry.max.risk.percent:2.5}")
	private double maxRiskPercent;

	/**
	 * Entry Signal Result containing all entry analysis
	 */
	public static class EntrySignal {
		public final double entryPrice;
		public final double confidence;
		public final String strategy;
		public final String signal; // BUY, SELL, HOLD
		public final double riskPercent;
		public final Map<String, Double> strategyScores;

		public EntrySignal(double entryPrice, double confidence, String strategy,
		                   String signal, double riskPercent,
		                   Map<String, Double> strategyScores) {
			this.entryPrice = entryPrice;
			this.confidence = confidence;
			this.strategy = strategy;
			this.signal = signal;
			this.riskPercent = riskPercent;
			this.strategyScores = strategyScores;
		}
	}

	/**
	 * Calculate optimal entry signal using 3 advanced strategies
	 */
	public void calculateOptimalEntry(TradeSetupTO tradeSetup,
	                                  List<Candle> historicalCandles,
	                                  Map<Integer, StrikeTO> strikes1) {
		List<StrikeTO> strikes = strikes1 != null ? new ArrayList<>(strikes1.values()) : new ArrayList<>();

		log.info("Calculating optimal entry for {} - Current Price: {}",
				tradeSetup.getStockSymbol(), tradeSetup.getEntry1());

		try {

			// Calculate individual strategy signals
			Map<String, Double> strategyScores = new HashMap<>();

			// Strategy 1: Breakout Confirmation (40% weight)
			double breakoutScore = calculateBreakoutConfirmationEntry(tradeSetup, historicalCandles);
			strategyScores.put("BC", breakoutScore);

			// Strategy 2: Mean Reversion (35% weight)
//			double meanReversionScore = calculateMeanReversionEntry(tradeSetup, historicalCandles);
//			strategyScores.put("MR", meanReversionScore);

			// Strategy 3: Volume-Price Analysis (25% weight)
//			double volumePriceScore = calculateVolumePriceEntry(tradeSetup, historicalCandles, strikes);
//			strategyScores.put("VAP", volumePriceScore);


			// Calculate weighted confidence score
//			double weightedConfidence = (breakoutScore * 0.40) +
//					(meanReversionScore * 0.35) +
//					(volumePriceScore * 0.25);

			// Determine optimal entry price and signal
//			double optimalEntryPrice = calculateOptimalEntryPrice(tradeSetup, historicalCandles, strategyScores);
//			String entrySignal = determineEntrySignal(weightedConfidence, strategyScores);
//			String primaryStrategy = determinePrimaryStrategy(strategyScores);

			// Add EntryInfo for each strategy with their respective scores and entry prices
			Map<String, Double> breakoutMap = new HashMap<>();
			breakoutMap.put("BC", breakoutScore);
			tradeSetup.getEntryInfos().add(new EntryInfo("BC",
					calculateOptimalEntryPrice(tradeSetup, historicalCandles, breakoutMap)));

			// Map<String, Double> meanReversionMap = new HashMap<>();
			// meanReversionMap.put("MR", meanReversionScore);
			// tradeSetup.getEntryInfos().add(new EntryInfo("MR", 
			// 	calculateOptimalEntryPrice(tradeSetup, historicalCandles, meanReversionMap)));

//			Map<String, Double> volumePriceMap = new HashMap<>();
//			volumePriceMap.put("VAP", volumePriceScore);
//			tradeSetup.getEntryInfos().add(new EntryInfo("VAP",
//					calculateOptimalEntryPrice(tradeSetup, historicalCandles, volumePriceMap)));

			// Keep the default D entry (renamed from E1)
			if (tradeSetup.getEntry1() != null) {
				tradeSetup.getEntryInfos().add(new EntryInfo("D", tradeSetup.getEntry1()));
			}

			// Calculate risk assessment
//			double riskPercent = calculateEntryRisk(optimalEntryPrice, tradeSetup, historicalCandles);

		} catch (Exception e) {
			log.error("Error calculating entry for {}: {}", tradeSetup.getStockSymbol(), e.getMessage(), e);
		}
	}

	/**
	 * Strategy 1: Breakout Confirmation Entry
	 * Identifies momentum-based entries when price breaks key levels with volume confirmation
	 */
	private double calculateBreakoutConfirmationEntry(TradeSetupTO tradeSetup, List<Candle> candles) {
		try {
			if (candles.size() < 10) return 50.0; // Neutral score

			double currentPrice = tradeSetup.getEntry1();
			Candle latestCandle = candles.get(candles.size() - 1);

			// Calculate key resistance/support levels from recent highs/lows
			List<Double> recentHighs = candles.stream()
					.skip(Math.max(0, candles.size() - 20))
					.mapToDouble(Candle::getHigh)
					.boxed()
					.collect(Collectors.toList());

			List<Double> recentLows = candles.stream()
					.skip(Math.max(0, candles.size() - 20))
					.mapToDouble(Candle::getLow)
					.boxed()
					.collect(Collectors.toList());

			double resistanceLevel = recentHighs.stream().mapToDouble(Double::doubleValue).max().orElse(currentPrice);
			double supportLevel = recentLows.stream().mapToDouble(Double::doubleValue).min().orElse(currentPrice);

			// Calculate average volume for volume confirmation
			double avgVolume = candles.stream()
					.skip(Math.max(0, candles.size() - 10))
					.mapToDouble(Candle::getVolume)
					.average()
					.orElse(1.0);

			double currentVolume = latestCandle.getVolume();

			// Breakout scoring logic
			double score = 50.0; // Base neutral score

			// Check for bullish breakout
			if (currentPrice > resistanceLevel * (1 + breakoutPriceBuffer / 100)) {
				score += 30; // Strong bullish signal

				// Volume confirmation bonus
				if (currentVolume > avgVolume * breakoutVolumeMultiplier) {
					score += 15; // Volume confirms breakout
				}

				// Momentum confirmation (price above previous close)
				if (latestCandle.getClose() > latestCandle.getOpen()) {
					score += 10; // Bullish candle
				}
			}
			// Check for bearish breakout
			else if (currentPrice < supportLevel * (1 - breakoutPriceBuffer / 100)) {
				score += 25; // Bearish breakout (slightly lower weight for long bias)

				if (currentVolume > avgVolume * breakoutVolumeMultiplier) {
					score += 10;
				}
			}
			// Check for consolidation near breakout levels
			else if (Math.abs(currentPrice - resistanceLevel) / resistanceLevel < 0.01) {
				score += 20; // Near resistance - potential breakout setup
			}

			return Math.min(95.0, Math.max(5.0, score));

		} catch (Exception e) {
			log.error("Error in breakout confirmation calculation: {}", e.getMessage());
			return 50.0;
		}
	}

	/**
	 * Strategy 2: Mean Reversion Entry
	 * Identifies counter-trend entries at oversold/overbought levels with strong support/resistance
	 */
	private double calculateMeanReversionEntry(TradeSetupTO tradeSetup, List<Candle> candles) {
		try {
			if (candles.size() < 14) return 50.0; // Need minimum for RSI calculation

			double currentPrice = candles.get(0).getClose();

			// Calculate RSI (14-period)
			double rsi = calculateRSI(candles, 14);

			// Calculate Bollinger Bands (20-period, 2 std dev)
			BollingerBands bb = calculateBollingerBands(candles, 20, 2.0);

			// Calculate distance from moving averages
			double sma20 = calculateSMA(candles, 20);
			double sma50 = candles.size() >= 50 ? calculateSMA(candles, 50) : sma20;

			double score = 50.0; // Base neutral score

			// RSI-based mean reversion signals
			if (rsi < meanReversionRSIOversold) {
				score += 25; // Oversold - potential bounce

				// Additional confirmation if near Bollinger lower band
				if (currentPrice <= bb.lowerBand * 1.01) {
					score += 15; // Strong oversold signal
				}

				// Support level confirmation
				if (currentPrice > sma20 * 0.98) {
					score += 10; // Above key moving average support
				}
			} else if (rsi > meanReversionRSIOverbought) {
				score += 20; // Overbought - potential pullback (lower weight for long bias)

				if (currentPrice >= bb.upperBand * 0.99) {
					score += 10;
				}
			}

			// Mean reversion to moving averages
			double distanceFromSMA20 = Math.abs(currentPrice - sma20) / sma20;
			if (distanceFromSMA20 > 0.05) { // More than 5% from SMA20
				if (currentPrice < sma20) {
					score += 15; // Below SMA20 - potential mean reversion up
				} else {
					score += 10; // Above SMA20 - potential mean reversion down
				}
			}

			// Bollinger Band squeeze detection (low volatility before expansion)
			double bandWidth = (bb.upperBand - bb.lowerBand) / bb.middleBand;
			List<Double> recentBandWidths = new ArrayList<>();
			for (int i = Math.max(0, candles.size() - 10); i < candles.size() - 1; i++) {
				BollingerBands historicalBB = calculateBollingerBands(candles.subList(0, i + 1), 20, 2.0);
				recentBandWidths.add((historicalBB.upperBand - historicalBB.lowerBand) / historicalBB.middleBand);
			}

			if (!recentBandWidths.isEmpty()) {
				double avgBandWidth = recentBandWidths.stream().mapToDouble(Double::doubleValue).average().orElse(bandWidth);
				if (bandWidth < avgBandWidth * 0.8) {
					score += 10; // Squeeze detected - potential breakout setup
				}
			}

			return Math.min(95.0, Math.max(5.0, score));

		} catch (Exception e) {
			log.error("Error in mean reversion calculation: {}", e.getMessage());
			return 50.0;
		}
	}

	/**
	 * Strategy 3: Volume-Price Analysis Entry
	 * Analyzes institutional flow through volume patterns and option chain data
	 */
	private double calculateVolumePriceEntry(TradeSetupTO tradeSetup, List<Candle> candles, List<StrikeTO> strikes) {
		try {
			if (candles.size() < 5) return 50.0;

			double currentPrice = candles.get(0).getClose();
			Candle latestCandle = candles.get(candles.size() - 1);

			double score = 50.0; // Base neutral score

			// Volume analysis
			double avgVolume = candles.stream()
					.skip(Math.max(0, candles.size() - 10))
					.mapToDouble(Candle::getVolume)
					.average()
					.orElse(1.0);

			double currentVolume = latestCandle.getVolume();
			double volumeRatio = currentVolume / avgVolume;

			// High volume confirmation
			if (volumeRatio > volumeAnalysisThreshold) {
				score += 20; // Institutional interest

				// Price-volume relationship
				double priceChange = (latestCandle.getClose() - latestCandle.getOpen()) / latestCandle.getOpen();
				if (priceChange > 0 && volumeRatio > 1.5) {
					score += 15; // Strong buying pressure
				} else if (priceChange < 0 && volumeRatio > 1.5) {
					score += 10; // Strong selling pressure
				}
			}

			// Volume-Weighted Average Price (VWAP) analysis
			double vwap = calculateVWAP(candles);
			if (currentPrice > vwap * 1.005) {
				score += 10; // Above VWAP - bullish
			} else if (currentPrice < vwap * 0.995) {
				score += 8; // Below VWAP - potential support
			}

			// Option chain analysis (if available)
			if (strikes != null && !strikes.isEmpty()) {
				double optionChainScore = analyzeOptionChainFlow(currentPrice, strikes);
				score += optionChainScore * 0.3; // 30% weight for option flow
			}

			// Volume profile analysis - find high volume nodes
			Map<Double, Double> volumeProfile = createVolumeProfile(candles);
			double nearestHighVolumeNode = findNearestHighVolumeNode(currentPrice, volumeProfile);

			if (Math.abs(currentPrice - nearestHighVolumeNode) / currentPrice < 0.02) {
				score += 12; // Near high volume node - institutional level
			}

			// Accumulation/Distribution analysis
			double adLine = calculateAccumulationDistribution(candles);
			List<Double> recentAD = new ArrayList<>();
			for (int i = Math.max(0, candles.size() - 5); i < candles.size(); i++) {
				recentAD.add(calculateAccumulationDistribution(candles.subList(0, i + 1)));
			}

			if (recentAD.size() >= 2) {
				double adTrend = recentAD.get(recentAD.size() - 1) - recentAD.get(0);
				if (adTrend > 0) {
					score += 8; // Accumulation trend
				} else if (adTrend < 0) {
					score += 5; // Distribution trend
				}
			}

			return Math.min(95.0, Math.max(5.0, score));

		} catch (Exception e) {
			log.error("Error in volume-price analysis calculation: {}", e.getMessage());
			return 50.0;
		}
	}

	/**
	 * Calculate optimal entry price based on strategy scores
	 */
	private double calculateOptimalEntryPrice(TradeSetupTO tradeSetup, List<Candle> candles,
	                                          Map<String, Double> strategyScores) {
		double currentPrice = candles.get(0).getClose();

		// Get the highest scoring strategy
		String topStrategy = strategyScores.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse("BC");

		// Adjust entry price based on top strategy
		switch (topStrategy) {
			case "BC":
				// For breakouts, enter slightly above resistance or below support
				return currentPrice * (1 + breakoutPriceBuffer / 200); // Half buffer for entry

			case "MR":
				// For mean reversion, enter at current price or slightly better
				return currentPrice * 0.999; // Slight discount

			case "VAP":
				// For volume analysis, enter at VWAP or current price
				double vwap = calculateVWAP(candles);
				return Math.min(currentPrice, vwap);

			default:
				return currentPrice;
		}
	}

	/**
	 * Determine entry signal based on confidence and strategy scores
	 */
	private String determineEntrySignal(double confidence, Map<String, Double> strategyScores) {
		if (confidence >= confidenceThreshold) {
			// Check if any strategy strongly suggests a specific direction
			double maxScore = strategyScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(50.0);

			if (maxScore >= 75) {
				return "BUY"; // Strong bullish signal
			} else if (maxScore >= 65) {
				return "BUY"; // Moderate bullish signal (default long bias)
			} else {
				return "HOLD"; // Wait for better setup
			}
		} else {
			return "HOLD"; // Low confidence
		}
	}

	/**
	 * Determine primary strategy contributing most to the signal
	 */
	private String determinePrimaryStrategy(Map<String, Double> strategyScores) {
		return strategyScores.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse("Mixed");
	}

	/**
	 * Calculate entry risk as percentage of entry price
	 */
	private double calculateEntryRisk(double entryPrice, TradeSetupTO tradeSetup, List<Candle> candles) {
		try {
			// Calculate ATR for volatility-based risk
			double atr = calculateATR(candles, 14);
			double atrRisk = (atr / entryPrice) * 100;

			// Calculate support-based risk
			double supportLevel = candles.stream()
					.skip(Math.max(0, candles.size() - 20))
					.mapToDouble(Candle::getLow)
					.min()
					.orElse(entryPrice * 0.95);

			double supportRisk = ((entryPrice - supportLevel) / entryPrice) * 100;

			// Use the lower of ATR-based or support-based risk, capped at max
			double calculatedRisk = Math.min(atrRisk, supportRisk);
			return Math.min(calculatedRisk, maxRiskPercent);

		} catch (Exception e) {
			log.error("Error calculating entry risk: {}", e.getMessage());
			return maxRiskPercent; // Conservative fallback
		}
	}

	/**
	 * Create fallback entry signal when calculation fails
	 */
	private EntrySignal createFallbackEntry(TradeSetupTO tradeSetup, String reason) {
		Map<String, Double> fallbackScores = new HashMap<>();
		fallbackScores.put("Fallback", 50.0);

		return new EntrySignal(
				tradeSetup.getEntry1(),
				50.0,
				"Fallback",
				"HOLD",
				maxRiskPercent,
				fallbackScores
		);
	}

	// ==================== TECHNICAL INDICATOR CALCULATIONS ====================

	private double calculateRSI(List<Candle> candles, int period) {
		if (candles.size() < period + 1) return 50.0;

		double gainSum = 0.0, lossSum = 0.0;

		for (int i = candles.size() - period; i < candles.size(); i++) {
			double change = candles.get(i).getClose() - candles.get(i - 1).getClose();
			if (change > 0) {
				gainSum += change;
			} else {
				lossSum += Math.abs(change);
			}
		}

		double avgGain = gainSum / period;
		double avgLoss = lossSum / period;

		if (avgLoss == 0) return 100.0;

		double rs = avgGain / avgLoss;
		return 100 - (100 / (1 + rs));
	}

	private static class BollingerBands {
		final double upperBand;
		final double middleBand;
		final double lowerBand;

		BollingerBands(double upper, double middle, double lower) {
			this.upperBand = upper;
			this.middleBand = middle;
			this.lowerBand = lower;
		}
	}

	private BollingerBands calculateBollingerBands(List<Candle> candles, int period, double stdDev) {
		if (candles.size() < period) {
			double price = candles.get(candles.size() - 1).getClose();
			return new BollingerBands(price * 1.02, price, price * 0.98);
		}

		double sma = calculateSMA(candles, period);

		double variance = candles.stream()
				.skip(Math.max(0, candles.size() - period))
				.mapToDouble(c -> Math.pow(c.getClose() - sma, 2))
				.average()
				.orElse(0.0);

		double standardDeviation = Math.sqrt(variance);

		return new BollingerBands(
				sma + (standardDeviation * stdDev),
				sma,
				sma - (standardDeviation * stdDev)
		);
	}

	private double calculateSMA(List<Candle> candles, int period) {
		return candles.stream()
				.skip(Math.max(0, candles.size() - period))
				.mapToDouble(Candle::getClose)
				.average()
				.orElse(0.0);
	}

	private double calculateVWAP(List<Candle> candles) {
		double totalVolume = 0.0;
		double totalVolumePrice = 0.0;

		for (Candle candle : candles) {
			double typicalPrice = (candle.getHigh() + candle.getLow() + candle.getClose()) / 3.0;
			totalVolumePrice += typicalPrice * candle.getVolume();
			totalVolume += candle.getVolume();
		}

		return totalVolume > 0 ? totalVolumePrice / totalVolume : 0.0;
	}

	private double calculateATR(List<Candle> candles, int period) {
		if (candles.size() < period + 1) return 0.0;

		double sum = 0.0;
		for (int i = candles.size() - period; i < candles.size(); i++) {
			Candle current = candles.get(i);
			Candle previous = candles.get(i - 1);

			double tr1 = current.getHigh() - current.getLow();
			double tr2 = Math.abs(current.getHigh() - previous.getClose());
			double tr3 = Math.abs(current.getLow() - previous.getClose());

			sum += Math.max(tr1, Math.max(tr2, tr3));
		}

		return sum / period;
	}

	private double analyzeOptionChainFlow(double currentPrice, List<StrikeTO> strikes) {
		if (strikes == null || strikes.isEmpty()) return 0.0;

		double callOI = 0.0, putOI = 0.0;
		double callVolume = 0.0, putVolume = 0.0;

		// Analyze strikes near current price (within 5%)
		for (StrikeTO strike : strikes) {
			double strikePrice = strike.getStrikePrice();
			if (Math.abs(strikePrice - currentPrice) / currentPrice <= 0.05) {
				callOI += strike.getCeOi();
				putOI += strike.getPeOi();
				callVolume += strike.getCeVolume();
				putVolume += strike.getPeVolume();
			}
		}

		// Calculate Put-Call ratio (lower = bullish, higher = bearish)
		double pcRatio = putOI > 0 ? callOI / putOI : 2.0;
		double pcVolumeRatio = putVolume > 0 ? callVolume / putVolume : 2.0;

		// Score based on option flow (0-20 range)
		double score = 0.0;

		if (pcRatio > 1.2 && pcVolumeRatio > 1.2) {
			score = 15.0; // Strong call bias
		} else if (pcRatio > 1.0 && pcVolumeRatio > 1.0) {
			score = 10.0; // Moderate call bias
		} else if (pcRatio < 0.8 && pcVolumeRatio < 0.8) {
			score = 5.0; // Put bias
		}

		return score;
	}

	private Map<Double, Double> createVolumeProfile(List<Candle> candles) {
		Map<Double, Double> profile = new HashMap<>();

		for (Candle candle : candles) {
			// Simplified: use close price as representative price
			double price = Math.round(candle.getClose() * 100) / 100.0; // Round to 2 decimals
			profile.merge(price, new Double(candle.getVolume()), (oldVal, newVal) -> oldVal + newVal);

		}

		return profile;
	}

	private double findNearestHighVolumeNode(double currentPrice, Map<Double, Double> volumeProfile) {
		if (volumeProfile.isEmpty()) return currentPrice;

		// Find the price level with highest volume near current price
		return volumeProfile.entrySet().stream()
				.filter(entry -> Math.abs(entry.getKey() - currentPrice) / currentPrice <= 0.1) // Within 10%
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(currentPrice);
	}

	private double calculateAccumulationDistribution(List<Candle> candles) {
		double adLine = 0.0;

		for (Candle candle : candles) {
			double clv = ((candle.getClose() - candle.getLow()) - (candle.getHigh() - candle.getClose()))
					/ (candle.getHigh() - candle.getLow());

			if (Double.isNaN(clv) || Double.isInfinite(clv)) {
				clv = 0.0; // Handle division by zero
			}

			adLine += clv * candle.getVolume();
		}

		return adLine;
	}
}
