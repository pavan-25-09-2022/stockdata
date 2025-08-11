package com.stocks.service;

import com.stocks.dto.HistoricalQuote;
import com.stocks.dto.MarketMoverData;
import com.stocks.dto.MarketMoversResponse;
import com.stocks.dto.Properties;
import com.stocks.dto.StrikeTO;
import com.stocks.dto.TradeSetupTO;
import com.stocks.repository.TradeSetupManager;
import com.stocks.utils.CalendarUtil;
import com.stocks.utils.DateUtil;
import com.stocks.utils.FormatUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
	@Autowired
	private YahooFinanceService yahooFinanceService;

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
			if ("G".equals(type) && (oiChg > 2 || oiChg < -2) && lptChgPer > 2) {
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
				threadSleep(333);
				LocalTime endTime1 = FormatUtil.getTimeHHmmss("15:00:00");
				LocalTime endTime = FormatUtil.getTime(startTime, interval);
				boolean isCriteria1Met = false;
				boolean isCriteria2Met = false;
				boolean isCriteria3Met = false;
				boolean isCriteria4Met = false;
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
						if (!isCriteria4Met) {
							TradeSetupTO tradeSetup3 = validateAndSetDetails(strikes, marketMoverData.getStSymbolName(), "criteria4");
							buildTradeSetupTO(tradeSetup3, marketMoverData, properties, oiChg, lptChgPer, oiInterpretation, value);
							if (isTradeProcessed(tradeSetup3)) {
								tradeSetup3.setStrikes(strikes);
								trades.add(tradeSetup3);
								log.info("stock {} criteria4 met", marketMoverData.getStSymbolName());
								isCriteria4Met = true;
							}
						}
						if (isCriteria1Met && isCriteria2Met && isCriteria3Met && isCriteria4Met) {
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

		StrikeTO peVolumeStrike = getLargestPeVolumeStrike(strikes);
		StrikeTO ceVolumeStrike = getLargestCeVolumeStrike(strikes);
		boolean allValid = isValidStrike(strikeUp3) && isValidStrike(strikeUp2) && isValidStrike(strikeUp1) &&
				isValidStrike(strikeDown1) && isValidStrike(strikeDown2) && isValidStrike(strikeDown3);
		TradeSetupTO tradeSetup = new TradeSetupTO();
		if ("criteria3".equals(criteria) && allValid && (strike0.getPeOiChg() > ((strike0.getCeOiChg()) * 0.7)) &&
				strikeUp2.getCeOiChg() < 0 && strikeUp3.getCeOiChg() < 0 &&
				(strikeDown1.getPeOiChg() + strikeDown2.getPeOiChg() + strikeDown3.getPeOiChg()) > 0) {
			tradeSetup.setStrategy(criteria);
			double val = (strike0.getStrikePrice() + strike0.getCurPrice()) / 2;
			if (val > strike0.getCurPrice()) {
				tradeSetup.setEntry1(strike0.getCurPrice());
				tradeSetup.setEntry2(val);
			} else {
				tradeSetup.setEntry1(val);
				tradeSetup.setEntry2(strike0.getStrikePrice());
			}
			setTargetPrices(tradeSetup, strikes);
			tradeSetup.setStopLoss1(strikeUp2.getStrikePrice());
			if (tradeSetup.getTarget1() <= strikes.get(0).getStrikePrice()) {
				return null;
			}
			return tradeSetup;
		} else if (criteria.equals("criteria2") && isValidStrike(strike0) && allValid && (strike0.getPeOiChg() > ((strike0.getCeOiChg()) * 0.7)) &&
				strikeUp1.getCeOiChg() < 0 && strikeUp2.getCeOiChg() < 0 &&
				strikeDown1.getPeOiChg() > 0 && strikeDown2.getPeOiChg() > 0) {
			tradeSetup.setEntry1((strike0.getStrikePrice() + strikeDown1.getStrikePrice()) / 2);
			tradeSetup.setEntry2(strike0.getStrikePrice());
			tradeSetup.setTarget1((strikeDown2.getStrikePrice() + strikeDown3.getStrikePrice()) / 2);
			tradeSetup.setTarget2(strikeDown3.getStrikePrice());
			tradeSetup.setStopLoss1(strikeUp2.getStrikePrice());
			tradeSetup.setStrategy(criteria);
			return tradeSetup;
		} else if (criteria.equals("criteria1") && isValidStrike(strike0) && allValid &&
				strikeUp1.getCeOiChg() < 0 && strikeUp2.getCeOiChg() < 0 && strikeUp3.getCeOiChg() < 0 &&
				strikeDown1.getPeOiChg() > 0 && strikeDown2.getPeOiChg() > 0 && strikeDown3.getPeOiChg() > 0) {
			tradeSetup.setEntry1((strike0.getStrikePrice() + strikeDown1.getStrikePrice()) / 2);
			tradeSetup.setEntry2(strike0.getStrikePrice());
			tradeSetup.setTarget1((strikeDown2.getStrikePrice() + strikeDown3.getStrikePrice()) / 2);
			tradeSetup.setTarget2(strikeDown3.getStrikePrice());
			tradeSetup.setStopLoss1(strikeUp2.getStrikePrice());
			tradeSetup.setStrategy(criteria);
			return tradeSetup;
		} else if (criteria.equals("criteria4") && isValidStrike(strike0) && allValid &&
				strikeUp1.getCeOiChg() < 0 && strikeUp2.getCeOiChg() < 0 &&
				strikeDown1.getPeOiChg() > 0 && strikeDown2.getPeOiChg() > 0 &&
				peVolumeStrike.getStrikePrice() <= strike0.getStrikePrice()
				&& ceVolumeStrike.getStrikePrice() > strike0.getStrikePrice()) {
			tradeSetup.setEntry1((strike0.getStrikePrice() + strikeDown1.getStrikePrice()) / 2);
			tradeSetup.setEntry2(strike0.getStrikePrice());
			tradeSetup.setTarget1(ceVolumeStrike.getStrikePrice());
			tradeSetup.setTarget2(strikeDown3.getStrikePrice());
			tradeSetup.setStopLoss1(peVolumeStrike.getStrikePrice());
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

	private void setTargetPrices(TradeSetupTO tradeSetupTO, Map<Integer, StrikeTO> strikes) {
		StrikeTO ceVolumeStrike = getLargestCeVolumeStrike(strikes);
		tradeSetupTO.setTarget1(ceVolumeStrike.getStrikePrice());
		Double target2Price = null;
		if (strikes.get(1) != null && strikes.get(1).getPeOiChg() < 0) {
			target2Price = strikes.get(1).getStrikePrice();
		} else if (strikes.get(2) != null && strikes.get(2).getPeOiChg() < 0) {
			target2Price = strikes.get(2).getStrikePrice();
		} else if (strikes.get(3) != null && strikes.get(3).getPeOiChg() < 0) {
			target2Price = strikes.get(3).getStrikePrice();
		} else if (strikes.get(4) != null && strikes.get(4).getPeOiChg() < 0) {
			target2Price = strikes.get(4).getStrikePrice();
		} else {
			target2Price = strikes.get(5).getStrikePrice();
		}
		tradeSetupTO.setTarget2(target2Price);
	}

	private boolean isValidStrike(StrikeTO strike) {
		if (strike == null) {
			return false;
		}
		String ceOiInt = strike.getCeOiInt();
		String peOiInt = strike.getPeOiInt();
		return ("SC".equals(ceOiInt) || "LBU".equals(ceOiInt)) &&
				("LU".equals(peOiInt) || "SBU".equals(peOiInt));
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

	public StrikeTO getLargestCeVolumeStrike(Map<Integer, StrikeTO> strikes) {
		if (strikes == null || strikes.isEmpty()) return null;
		StrikeTO maxStrike = null;
		int maxCeVolume = Integer.MIN_VALUE;
		for (StrikeTO strike : strikes.values()) {
			if (strike != null && strike.getCeVolume() > maxCeVolume) {
				maxCeVolume = strike.getCeVolume();
				maxStrike = strike;
			}
		}
		return maxStrike;
	}

	public StrikeTO getLargestPeVolumeStrike(Map<Integer, StrikeTO> strikes) {
		if (strikes == null || strikes.isEmpty()) return null;
		StrikeTO maxStrike = null;
		int maxPeVolume = Integer.MIN_VALUE;
		for (StrikeTO strike : strikes.values()) {
			if (strike != null && strike.getPeVolume() > maxPeVolume) {
				maxPeVolume = strike.getPeVolume();
				maxStrike = strike;
			}
		}
		return maxStrike;
	}

	public List<TradeSetupTO> testPositiveMarketMovers(Properties properties) {
		List<TradeSetupTO> list = tradeSetupManager.findTradeSetupByDate(properties.getStockDate());
		for (TradeSetupTO trade : list) {
			if (StringUtils.hasLength(properties.getStockName()) && !trade.getStockSymbol().equals(properties.getStockName())) {
				continue;
			}
			try {
				String date = trade.getStockDate();
				String time = trade.getFetchTime();
				int noOfDays = 7;
				int entryDays = 1;
				boolean isEntry1 = StringUtils.hasLength(trade.getEntry1Time());
				boolean isEntry2 = StringUtils.hasLength(trade.getEntry2Time());
				boolean isTarget1 = StringUtils.hasLength(trade.getTarget1Time());
				boolean isTarget2 = StringUtils.hasLength(trade.getTarget2Time());
				boolean isStopLoss1 = StringUtils.hasLength(trade.getStopLoss1Time());
				for (int i = 0; i < noOfDays; i++) {
					if (!isEntry1 && !isEntry2 && i > entryDays) {
						trade.setTradeNotes("N-E (2D)");
						trade.setStatus("NO ENTRY");
						break;
					}
					if ((isTarget1 && isTarget2) || isStopLoss1) {
						break;
					}
					Calendar from = null;
					if (i == 0) {
						from = CalendarUtil.buildCalendar(date, time, i, 5);
					} else {
						from = CalendarUtil.buildCalendar(date, "09:15", i, 0);
					}
					Calendar to = CalendarUtil.buildCalendar(date, "15:30", i, 0);
					List<HistoricalQuote> candles = yahooFinanceService.getHistoricalQuote(trade.getStockSymbol(), from, to, properties.getInterval() + "");
					if (candles != null && candles.size() > 1) {
						candles = candles.subList(0, candles.size() - 1);
						for (int j = 0; j < candles.size(); j++) {
							HistoricalQuote candle = candles.get(j);
							if (candle.getHigh() == null || candle.getLow() == null || candle.getHigh().intValue() == 0 || candle.getLow().intValue() == 0) {
								continue; // Skip quotes with null values
							}
							isEntry1 = StringUtils.hasLength(trade.getEntry1Time());
							isEntry2 = StringUtils.hasLength(trade.getEntry2Time());
							isTarget1 = StringUtils.hasLength(trade.getTarget1Time());
							isTarget2 = StringUtils.hasLength(trade.getTarget2Time());
							isStopLoss1 = StringUtils.hasLength(trade.getStopLoss1Time());
							if ((isTarget1 && isTarget2) || isStopLoss1) {
								break;
							}
							double candleLow = candle.getLow().doubleValue();
							double candleHigh = candle.getHigh().doubleValue();
							if (!isEntry1 && candleLow <= trade.getEntry1()) {
								trade.setEntry1Time(DateUtil.getDateTimeFromCalendar(candle.getDate()));
								trade.setTradeNotes(trade.getTradeNotes() != null ? (trade.getTradeNotes() + "E1 ") : "E1 ");
								trade.setStatus("O");
								isEntry1 = true;
							}
							if (!isEntry1 && !isEntry2 && trade.getEntry2() != null && candleLow <= trade.getEntry2()) {
								trade.setEntry2Time(DateUtil.getDateTimeFromCalendar(candle.getDate()));
								trade.setTradeNotes(trade.getTradeNotes() + "E2 ");
								trade.setStatus("O");
								isEntry2 = true;
							}
							if ((isEntry1 || isEntry2) && !isTarget1 && trade.getTarget1() != null && candleHigh >= trade.getTarget1()) {
								trade.setTarget1Time(DateUtil.getDateTimeFromCalendar(candle.getDate()));
								trade.setTradeNotes(trade.getTradeNotes() + "T1 ");
								trade.setStatus("P");
								isTarget1 = true;
							}
							if ((isEntry1 || isEntry2) && !isTarget2 && trade.getTarget2() != null && candleHigh >= trade.getTarget2()) {
								trade.setTarget2Time(DateUtil.getDateTimeFromCalendar(candle.getDate()));
								trade.setTradeNotes(trade.getTradeNotes() + "T2 ");
								trade.setStatus("P");
								isTarget2 = true;
							}
							if ((isEntry1 || isEntry2) && (!isTarget1 || !isTarget2) && candleLow <= trade.getStopLoss1()) {
								trade.setStopLoss1Time(DateUtil.getDateTimeFromCalendar(candle.getDate()));
								trade.setTradeNotes(trade.getTradeNotes() + "SL ");
								if (isTarget1) {
									trade.setStatus("P");
								} else {
									trade.setStatus("L");
								}
								isStopLoss1 = true;
							}
						}
					}
				}
			} catch (IOException e) {
				log.error("Error fetching market mover {} : {}", trade.getStockSymbol(), e.getMessage(), e);
			}
			if (StringUtils.hasLength(trade.getTarget2Time()) && trade.getTarget2() != null) {
				if (StringUtils.hasLength(trade.getEntry1Time())) {
					double value = ((trade.getTarget2() - trade.getEntry1()) * 100) / trade.getEntry1();
					trade.setStatus("P " + String.format("%.2f", value));
				}
			} else if (StringUtils.hasLength(trade.getTarget1Time()) && trade.getTarget1() != null) {
				if (StringUtils.hasLength(trade.getEntry1Time())) {
					double value = ((trade.getTarget1() - trade.getEntry1()) * 100) / trade.getEntry1();
					trade.setStatus("P " + String.format("%.2f", value));
				}
			} else if (StringUtils.hasLength(trade.getStopLoss1Time()) && trade.getStopLoss1() != null) {
				if (StringUtils.hasLength(trade.getEntry1Time())) {
					double value = ((trade.getEntry1() - trade.getStopLoss1()) * 100) / trade.getEntry1();
					trade.setStatus("L " + String.format("%.2f", value));
				}
			}
		}
		return list;
	}

	public List<TradeSetupTO> marketMoverDetailsBasedOnVolume(Properties properties, String type) {

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
			if ("G".equals(type) && (oiChg > 1 || oiChg < -1) && lptChgPer > 0) {
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
				for (int i = interval; endTime.isBefore(endTime1); i += interval) {
					endTime = FormatUtil.getTime(startTime, i);
					if (properties.getStockDate().equals(LocalDate.now().toString()) && endTime.isAfter(LocalTime.now())) {
						break;
					}
					properties.setEndTime(endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
					Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, marketMoverData.getStSymbolName());

					if (strikes != null && !strikes.isEmpty() && strikes.get(1) != null && strikes.get(-1) != null) {
						int highCEVolumeAt = getHighVolumeCeStrikeIndex(strikes);
						int highPEVolumeAt = getHighVolumePeStrikeIndex(strikes);
						int ceVolume = getTotalCeVolume(strikes);
						int peVolume = getTotalPeVolume(strikes);
						StrikeTO highCeVolumeStrike = getLargestCeVolumeStrike(strikes);
						StrikeTO highPeVolumeStrike = getLargestPeVolumeStrike(strikes);
						/*if ( ceVolume > (3 * peVolume) ) {
							System.out.println("Stock " + stock + " time at " + endTime + "CE Volume: " + ceVolume + ", PE Volume: " + peVolume);
						}*/

						if (ceVolume > (3 * peVolume) && highCeVolumeStrike.getCeVolume() > (2 * highPeVolumeStrike.getPeVolume())) {
							System.out.println("Stock " + stock + " time at " + endTime + "CE Volume: " + ceVolume + ", PE Volume: " + peVolume + " High CE Strike: " + highCeVolumeStrike.getStrikePrice() + " High PE Strike: " + highPeVolumeStrike.getStrikePrice());
							TradeSetupTO tradeSetup = new TradeSetupTO();
							tradeSetup.setStockDate(properties.getStockDate());
							tradeSetup.setFetchTime(properties.getEndTime());
							tradeSetup.setStockSymbol(stock);
							tradeSetup.setEntry1((strikes.get(0).getStrikePrice() + strikes.get(1).getStrikePrice())/2);
							tradeSetup.setTarget1(strikes.get(2).getStrikePrice());
							tradeSetup.setTarget2(strikes.get(3).getStrikePrice());
							tradeSetup.setStrategy("VolumeBased");
							double stopLoss = Math.min(highPeVolumeStrike.getStrikePrice() , (strikes.get(-1).getStrikePrice() + strikes.get(-2).getStrikePrice()) / 2);
							tradeSetup.setStopLoss1(stopLoss);
							tradeSetup.setStrikes(strikes);
							trades.add(tradeSetup);
							break;
						}
					}
				}
			}
		}
		persistTrades(trades);
		return trades;
	}

	public int getTotalCeVolume(Map<Integer, StrikeTO> strikes) {
		int totalCeVolume = 0;
		for (int i = -3; i <= 3; i++) {
			StrikeTO strike = strikes.get(i);
			if (strike != null) {
				totalCeVolume += strike.getCeVolume();
			}
		}
		return totalCeVolume;
	}

	public int getTotalPeVolume(Map<Integer, StrikeTO> strikes) {
		int totalPeVolume = 0;
		for (int i = -3; i <= 3; i++) {
			StrikeTO strike = strikes.get(i);
			if (strike != null) {
				totalPeVolume += strike.getPeVolume();
			}
		}
		return totalPeVolume;
	}

	public int getHighVolumeCeStrikeIndex(Map<Integer, StrikeTO> strikes) {
		int maxVolume = Integer.MIN_VALUE;
		int maxIndex = 0;
		for (int i = -3; i <= 3; i++) {
			StrikeTO strike = strikes.get(i);
			if (strike != null && strike.getCeVolume() > maxVolume) {
				maxVolume = strike.getCeVolume();
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	public int getHighVolumePeStrikeIndex(Map<Integer, StrikeTO> strikes) {
		int maxVolume = Integer.MIN_VALUE;
		int maxIndex = 0;
		for (int i = -3; i <= 3; i++) {
			StrikeTO strike = strikes.get(i);
			if (strike != null && strike.getPeVolume() > maxVolume) {
				maxVolume = strike.getPeVolume();
				maxIndex = i;
			}
		}
		return maxIndex;
	}

}
