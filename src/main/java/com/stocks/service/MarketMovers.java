package com.stocks.service;

import com.stocks.dto.MarketMoverData;
import com.stocks.dto.MarketMoversResponse;
import com.stocks.dto.Properties;
import com.stocks.dto.StrikeTO;
import com.stocks.dto.TradeSetupTO;
import com.stocks.repository.TradeSetupManager;
import com.stocks.utils.FormatUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class MarketMovers {

	@Autowired
	private IOPulseService ioPulseService;
	@Autowired
	private CalculateOptionChain calculateOptionChain;
	@Autowired
	private TradeSetupManager tradeSetupManager;

	List<String> sectorList = Arrays.asList("NIFTY", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SMALLCAP", "NIFTYIT", "NIFTYFMCG", "NIFTYMETAL", "NIFTYPHARMA");


	public List<TradeSetupTO> marketMoverDetails(Properties properties, String type) {

		MarketMoversResponse marketMoversResponse = ioPulseService.marketMovers(properties);
		List<TradeSetupTO> trades = new ArrayList<>();
		if (marketMoversResponse == null || marketMoversResponse.getData() == null || marketMoversResponse.getData().isEmpty()) {
			log.error("No Market Movers data found");
			return trades;
		}

		for (MarketMoverData marketMoverData : marketMoversResponse.getData()) {
			String stock = marketMoverData.getStSymbolName();
			if (marketMoverData.getInOldOi() == null || marketMoverData.getInNewOi() == null ||
					marketMoverData.getInOldClose() == null || marketMoverData.getInNewClose() == null) {
				log.error("OI or Close data is missing for stock: {}", stock);
				continue;
			}
			if (sectorList.contains(stock)) {
				log.info("Skipping sector stock: {}", stock);
				continue;
			}
			double oldOi = Double.parseDouble(marketMoverData.getInOldOi());
			double newOi = Double.parseDouble(marketMoverData.getInNewOi());
			double oldClose = Double.parseDouble(marketMoverData.getInOldClose());
			double newClose = Double.parseDouble(marketMoverData.getInNewClose());
			double ltpChg = newClose - oldClose;
			double lptChgPer = (ltpChg / oldClose) * 100;
			double oiChg = ((newOi - oldOi) / oldOi) * 100;
			String oiInterpretation = (oiChg > 0)
					? (ltpChg > 0 ? "LBU" : "SBU")
					: (ltpChg > 0 ? "SC" : "LU");
			String value = null;
			if ("G".equals(type) && (oiChg > 2 || oiChg < -2)  && lptChgPer > 1) {
				value = "positive";
			} else if ("L".equals(type) && oiChg < -1) {
				value = "negative";
			} else {
				log.info("Stock {} Oi Change Per {} Ltp change per {}", stock, oiChg, lptChgPer);
				continue;
			}
			log.info("Stock {} with OI Change {}", stock, oiChg);
			String startTime = "09:15:00";
			int interval = properties.getInterval();
			//properties.setEndTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
			properties.setStartTime(startTime);
			properties.setEndTime(FormatUtil.getTime(properties.getStartTime(), interval).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
			properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));
			if (!properties.getStockName().isEmpty() && !stock.equals(properties.getStockName())) {
				continue;
			}
			if ("test".equals(properties.getEnv())) {
				threadSleep(100);
				LocalTime endTime1 = FormatUtil.getTimeHHmmss("15:00:00");
				LocalTime endTime = FormatUtil.getTime(startTime, interval);
				boolean isCriteria1Met = false;
				boolean isCriteria2Met = false;
				boolean isCriteria3Met = false;
				for (int i = interval; endTime.isBefore(endTime1); i += interval) {
					endTime = FormatUtil.getTime(startTime, i);
					if (properties.getStockDate().equals(LocalDate.now().toString()) && endTime.isAfter(LocalTime.now())) {
						break;
					}
					properties.setEndTime(endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
					Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, marketMoverData.getStSymbolName());
					if (strikes != null && !strikes.isEmpty()) {
						if (!isCriteria1Met) {
							TradeSetupTO tradeSetup1 = validateAndSetDetails(strikes, marketMoverData.getStSymbolName(), "criteria1");
							buildTradeSetupTO(tradeSetup1, marketMoverData, properties, oiChg, lptChgPer, oiInterpretation, value);
							if (isTradeProcessed(tradeSetup1)) {
								tradeSetup1.setStrikes(strikes);
								trades.add(tradeSetup1);
								log.info("stock {} criteria1 met", marketMoverData.getStSymbolName());
								isCriteria1Met = true;
							}
						}
						if (!isCriteria2Met) {
							TradeSetupTO tradeSetup2 = validateAndSetDetails(strikes, marketMoverData.getStSymbolName(), "criteria2");
							buildTradeSetupTO(tradeSetup2, marketMoverData, properties, oiChg, lptChgPer, oiInterpretation, value);
							if (isTradeProcessed(tradeSetup2)) {
								tradeSetup2.setStrikes(strikes);
								trades.add(tradeSetup2);
								log.info("stock {} criteria2 met", marketMoverData.getStSymbolName());
								isCriteria2Met = true;
							}
						}
						if (!isCriteria3Met) {
							TradeSetupTO tradeSetup3 = validateAndSetDetails(strikes, marketMoverData.getStSymbolName(), "criteria3");
							buildTradeSetupTO(tradeSetup3, marketMoverData, properties, oiChg, lptChgPer, oiInterpretation, value);
							if (isTradeProcessed(tradeSetup3)) {
								tradeSetup3.setStrikes(strikes);
								trades.add(tradeSetup3);
								log.info("stock {} criteria3 met", marketMoverData.getStSymbolName());
								isCriteria3Met = true;
							}
						}
						if (isCriteria1Met && isCriteria2Met && isCriteria3Met) {
							break;
						}
					}
				}
			} else {
				threadSleep(100);
				properties.setEndTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
				Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, marketMoverData.getStSymbolName());
				if (strikes != null && !strikes.isEmpty()) {
					TradeSetupTO tradeSetup1 = validateAndSetDetails(strikes, marketMoverData.getStSymbolName(), "criteria1");
					buildTradeSetupTO(tradeSetup1, marketMoverData, properties, oiChg, lptChgPer, oiInterpretation, value);
					if (isTradeProcessed(tradeSetup1)) {
						tradeSetup1.setStrikes(strikes);
						trades.add(tradeSetup1);
					}
					TradeSetupTO tradeSetup2 = validateAndSetDetails(strikes, marketMoverData.getStSymbolName(), "criteria2");
					buildTradeSetupTO(tradeSetup2, marketMoverData, properties, oiChg, lptChgPer, oiInterpretation, value);
					if (isTradeProcessed(tradeSetup2)) {
						tradeSetup2.setStrikes(strikes);
						trades.add(tradeSetup2);
					}
					TradeSetupTO tradeSetup3 = validateAndSetDetails(strikes, marketMoverData.getStSymbolName(), "criteria3");
					buildTradeSetupTO(tradeSetup3, marketMoverData, properties, oiChg, lptChgPer, oiInterpretation, value);
					if (isTradeProcessed(tradeSetup3)) {
						tradeSetup3.setStrikes(strikes);
						trades.add(tradeSetup3);
					}
				}
			}
		}
		persistTrades(trades);
		return trades;
	}

	private boolean isTradeProcessed(TradeSetupTO tradeSetup) {
		return (tradeSetup != null && tradeSetupManager.findTradeSetupByStockAndStrategyAndDate(tradeSetup.getStockSymbol(), tradeSetup.getStrategy(), tradeSetup.getStockDate()) == null);
	}

	private void persistTrades(List<TradeSetupTO> trades) {
		tradeSetupManager.saveTradeSetups(trades);
	}

	private TradeSetupTO validateAndSetDetails(Map<Integer, StrikeTO> strikes, String stock, String criteria) {
		if (strikes == null || strikes.isEmpty()) {
			log.error("No strikes found for stock: {}", stock);
			return null;
		}
		StrikeTO strikeUp3 = strikes.getOrDefault(-3, null);
		StrikeTO strikeUp2 = strikes.getOrDefault(-2, null);
		StrikeTO strikeUp1 = strikes.getOrDefault(-1, null);
		StrikeTO strike0 = strikes.getOrDefault(0, null);
		StrikeTO strikeDown1 = strikes.getOrDefault(1, null);
		StrikeTO strikeDown2 = strikes.getOrDefault(2, null);
		StrikeTO strikeDown3 = strikes.getOrDefault(3, null);
		StrikeTO strikeDown4 = strikes.getOrDefault(4, null);
		StrikeTO strikeDown5 = strikes.getOrDefault(5, null);
		StrikeTO strikeDown6 = strikes.getOrDefault(6, null);
		boolean allValid = isValidStrike(strikeUp3) && isValidStrike(strikeUp2) && isValidStrike(strikeUp1) &&
				isValidStrike(strikeDown1) && isValidStrike(strikeDown2) && isValidStrike(strikeDown3);
		TradeSetupTO tradeSetup = new TradeSetupTO();
		if ("criteria3".equals(criteria) && allValid && (strike0.getPeOiChg() > ((strike0.getCeOiChg()) * 0.7)) &&
				strikeUp1.getCeOiChg() <= 0 && strikeUp2.getCeOiChg() <= 0 && strikeUp3.getCeOiChg() <= 0 &&
				(strikeDown1.getPeOiChg() + strikeDown2.getPeOiChg() + strikeDown3.getPeOiChg()) >= 0) {
			tradeSetup.setStrategy(criteria);
			tradeSetup.setEntry1((strike0.getStrikePrice() + strike0.getCurPrice()) / 2);
			tradeSetup.setEntry2(strike0.getStrikePrice());
			setTargetPrices(tradeSetup, strike0, strikeDown1, strikeDown2, strikeDown3, strikeDown4, strikeDown5, strikeDown6);
			tradeSetup.setStopLoss1((strikeUp2.getStrikePrice() + strikeUp3.getStrikePrice()) / 2);
			return tradeSetup;
		} else if (criteria.equals("criteria2") && isValidStrike(strike0) && allValid && (strike0.getPeOiChg() > ((strike0.getCeOiChg()) * 0.7)) &&
				strikeUp1.getCeOiChg() <= 0 && strikeUp2.getCeOiChg() <= 0 &&
				strikeDown1.getPeOiChg() > 0 && strikeDown2.getPeOiChg() >= 0) {
			tradeSetup.setEntry1((strike0.getStrikePrice() + strikeDown1.getStrikePrice()) / 2);
			tradeSetup.setEntry2(strike0.getStrikePrice());
			tradeSetup.setTarget1((strikeDown2.getStrikePrice() + strikeDown3.getStrikePrice()) / 2);
			tradeSetup.setTarget2(strikeDown3.getStrikePrice());
			tradeSetup.setStopLoss1(strikeUp3.getStrikePrice());
			tradeSetup.setStrategy(criteria);
			return tradeSetup;
		} else if (criteria.equals("criteria1") && isValidStrike(strike0) && allValid && (strike0.getPeOiChg() > ((strike0.getCeOiChg()) * 0.7)) &&
				strikeUp1.getCeOiChg() <= 0 && strikeUp2.getCeOiChg() <= 0 && strikeUp3.getCeOiChg() <= 0 &&
				strikeDown1.getPeOiChg() > 0 && strikeDown2.getPeOiChg() >= 0 && strikeDown3.getPeOiChg() >= 0) {
			tradeSetup.setEntry1((strike0.getStrikePrice() + strikeDown1.getStrikePrice()) / 2);
			tradeSetup.setEntry2(strike0.getStrikePrice());
			tradeSetup.setTarget1((strikeDown2.getStrikePrice() + strikeDown3.getStrikePrice()) / 2);
			tradeSetup.setTarget2(strikeDown3.getStrikePrice());
			tradeSetup.setStopLoss1(strikeUp3.getStrikePrice());
			tradeSetup.setStrategy(criteria);
			return tradeSetup;
		}
		return null;
	}

	private void buildStopLoss(TradeSetupTO tradeSetupTO, StrikeTO strikeUp1, StrikeTO strikeUp2, StrikeTO strikeUp3) {
		Double stopLoss = null;
		double highestPeOiChg = strikeUp1.getPeOiChg();
		if (strikeUp2.getPeOiChg() > highestPeOiChg) {
			stopLoss = strikeUp2.getStrikePrice();
			highestPeOiChg = strikeUp2.getPeOiChg();
		}
		if (strikeUp3.getPeOiChg() > highestPeOiChg) {
			stopLoss = strikeUp3.getStrikePrice();
		}
		if (stopLoss == null) {
			stopLoss = strikeUp2.getStrikePrice();
		}

		tradeSetupTO.setStopLoss1(stopLoss);
	}

	private void setTargetPrices(TradeSetupTO tradeSetupTO, StrikeTO strike0, StrikeTO strikeDown1, StrikeTO strikeDown2, StrikeTO strikeDown3, StrikeTO strikeDown4, StrikeTO strikeDown5, StrikeTO strikeDown6) {
		Double target2Price = null;
		if (strikeDown1.getPeOiChg() < 0) {
			target2Price = strike0.getStrikePrice();
		} else if (strikeDown2.getPeOiChg() < 0) {
			target2Price = (strikeDown2.getStrikePrice() + strikeDown1.getStrikePrice()) / 2;
		} else if (strikeDown3.getPeOiChg() < 0) {
			target2Price = (strikeDown3.getStrikePrice() + strikeDown2.getStrikePrice()) / 2;
		} else if (strikeDown4.getPeOiChg() < 0) {
			target2Price = (strikeDown4.getStrikePrice() + strikeDown3.getStrikePrice()) / 2;
		} else if (strikeDown5.getPeOiChg() < 0) {
			target2Price = (strikeDown5.getStrikePrice() + strikeDown4.getStrikePrice()) / 2;
		} else if (strikeDown6.getPeOiChg() < 0) {
			target2Price = (strikeDown6.getStrikePrice() + strikeDown5.getStrikePrice()) / 2;
		}

		double highestCeOiChg = strike0.getCeOiChg();
		Double target1Price = strike0.getStrikePrice();
		if (strikeDown1.getCeOiChg() > highestCeOiChg) {
			target1Price = (strikeDown1.getStrikePrice() + strikeDown2.getStrikePrice()) / 2;
			highestCeOiChg = strikeDown1.getCeOiChg();
		}
		if (strikeDown2.getCeOiChg() > highestCeOiChg) {
			target1Price = (strikeDown2.getStrikePrice() + strikeDown3.getStrikePrice()) / 2;
			highestCeOiChg = strikeDown2.getCeOiChg();
		}
		if (strikeDown3.getCeOiChg() > highestCeOiChg) {
			target1Price = (strikeDown3.getStrikePrice() + strikeDown4.getStrikePrice()) / 2;
			highestCeOiChg = strikeDown3.getCeOiChg();
		}
		if (strikeDown4.getCeOiChg() > highestCeOiChg) {
			target1Price = (strikeDown4.getStrikePrice() + strikeDown5.getStrikePrice()) / 2;
			highestCeOiChg = strikeDown4.getCeOiChg();
		}
		if (strikeDown5.getCeOiChg() > highestCeOiChg) {
			target1Price = (strikeDown5.getStrikePrice() + strikeDown6.getStrikePrice()) / 2;
			highestCeOiChg = strikeDown5.getCeOiChg();
		}
		if (strikeDown6.getCeOiChg() > highestCeOiChg) {
			target1Price = strikeDown6.getStrikePrice();
		}
		tradeSetupTO.setTarget1(target1Price);
		tradeSetupTO.setTarget2(target2Price);
	}

	private boolean isValidStrike(StrikeTO strike) {
		if (strike == null) {
			return false;
		}
		String ceOiInt = strike.getCeOiInt();
		String peOiInt = strike.getPeOiInt();
		return ("SC".equals(ceOiInt) || "LBU".equals(ceOiInt) || ceOiInt == null || ceOiInt.isEmpty()) &&
				("LU".equals(peOiInt) || "SBU".equals(peOiInt) || "".equals(peOiInt) || peOiInt == null);
	}

	private void buildTradeSetupTO(TradeSetupTO tradeSetupTO, MarketMoverData marketMoverData, Properties prop, double oiChg, double ltpChg, String oiInterpretation, String value) {
		if (tradeSetupTO != null) {
			tradeSetupTO.setStockSymbol(marketMoverData.getStSymbolName());
			tradeSetupTO.setFetchTime(prop.getEndTime());
			tradeSetupTO.setStockDate(prop.getStockDate());
			tradeSetupTO.setOiChgPer(oiChg);
			tradeSetupTO.setLtpChgPer(ltpChg);
			tradeSetupTO.setType(value);
		}
	}

	private void threadSleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			log.error("Thread sleep interrupted", e);
			Thread.currentThread().interrupt(); // Restore interrupted status
		}
	}
}
