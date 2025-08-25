package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.HistoricalQuote;
import com.stocks.dto.MarketMoverData;
import com.stocks.dto.MarketMoversResponse;
import com.stocks.dto.Properties;
import com.stocks.dto.StrikeTO;
import com.stocks.dto.TradeSetupTO;
import com.stocks.repository.TradeSetupManager;
import com.stocks.utils.CalendarUtil;
import com.stocks.utils.CommonUtils;
import com.stocks.utils.FormatUtil;
import com.stocks.utils.MarketHolidayUtils;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	@Autowired
	private CommonUtils commonUtils;
	@Autowired
	private CommonValidation commonValidation;

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
			List<String> values = getStockType(marketMoverData, type);
			if (values == null) {
				continue;
			}
			List<TradeSetupTO> list = processTradeSetup(properties, values, stock);
			trades.addAll(list);
		}
		persistTrades(trades);
		if (properties.getStrategy() != null) {
			return trades.stream()
					.filter(tradeSetupTO -> {
						if (tradeSetupTO.getStrategy() == null) {
							return false;
						}
						return tradeSetupTO.getStrategy().equals(properties.getStrategy());
					}).collect(Collectors.toList());
		}
		return trades;
	}

	private List<TradeSetupTO> processTradeSetup(Properties properties, List<String> values, String stock) {
		List<TradeSetupTO> trades = new ArrayList<>();
		String startTime = "09:15:00";
		int interval = properties.getInterval();
		//properties.setEndTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		properties.setStartTime(startTime);
		properties.setEndTime(FormatUtil.getTime(properties.getStartTime(), interval).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));
		if (!properties.getStockName().isEmpty() && !stock.equals(properties.getStockName())) {
			return trades;
		}
		if ("test".equals(properties.getEnv())) {
			threadSleep(50);
			LocalTime endTime1 = FormatUtil.getTimeHHmmss("12:00:00");
			LocalTime endTime = FormatUtil.getTime(startTime, interval);
			boolean isCriteria1Met = StringUtils.hasText(properties.getStrategy()) && !"c1".equals(properties.getStrategy());
			boolean isCriteria2Met = StringUtils.hasText(properties.getStrategy()) && !"c2".equals(properties.getStrategy());
			boolean isCriteria3Met = StringUtils.hasText(properties.getStrategy()) && !"c3".equals(properties.getStrategy());
			boolean isCriteria4Met = StringUtils.hasText(properties.getStrategy()) && !"c4".equals(properties.getStrategy());
			for (int i = interval; endTime.isBefore(endTime1); i += interval) {
				endTime = FormatUtil.getTime(startTime, i);
				if (properties.getStockDate().equals(LocalDate.now().toString()) && endTime.isAfter(LocalTime.now())) {
					break;
				}
				properties.setEndTime(endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
				Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, stock);
				if (strikes != null && !strikes.isEmpty()) {
					if (!isCriteria1Met) {
						TradeSetupTO tradeSetup1 = validateAndSetDetails(strikes, stock, "c1");
						buildTradeSetupTO(tradeSetup1, properties, values, stock);
						if (isTradeProcessed(tradeSetup1)) {
							tradeSetup1.setStrikes(strikes);
							trades.add(tradeSetup1);
							log.info("stock {} criteria1 met", stock);
							isCriteria1Met = true;
						}
					}
					if (!isCriteria2Met) {
						TradeSetupTO tradeSetup2 = validateAndSetDetails(strikes, stock, "c2");
						buildTradeSetupTO(tradeSetup2, properties, values, stock);
						if (isTradeProcessed(tradeSetup2)) {
							tradeSetup2.setStrikes(strikes);
							trades.add(tradeSetup2);
							log.info("stock {} criteria2 met", stock);
							isCriteria2Met = true;
						}
					}
					if (!isCriteria3Met) {
						TradeSetupTO tradeSetup3 = validateAndSetDetails(strikes, stock, "c3");
						buildTradeSetupTO(tradeSetup3, properties, values, stock);
						if (isTradeProcessed(tradeSetup3)) {
							tradeSetup3.setStrikes(strikes);
							trades.add(tradeSetup3);
							log.info("stock {} criteria3 met", stock);
							isCriteria3Met = true;
						}
					}
					if (!isCriteria4Met) {
						TradeSetupTO tradeSetup3 = validateAndSetDetails(strikes, stock, "c4");
						buildTradeSetupTO(tradeSetup3, properties, values, stock);
						if (isTradeProcessed(tradeSetup3)) {
							tradeSetup3.setStrikes(strikes);
							trades.add(tradeSetup3);
							log.info("stock {} criteria4 met", stock);
							isCriteria4Met = true;
						}
					}
					if (isCriteria1Met && isCriteria2Met && isCriteria3Met && isCriteria4Met) {
						break;
					}
				}
			}
		} else {
//				threadSleep(100);
			properties.setEndTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
			Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, stock);
			if (strikes != null && !strikes.isEmpty()) {
				TradeSetupTO tradeSetup1 = validateAndSetDetails(strikes, stock, "c1");
				buildTradeSetupTO(tradeSetup1, properties, values, stock);
				if (isTradeProcessed(tradeSetup1)) {
					tradeSetup1.setStrikes(strikes);
					trades.add(tradeSetup1);
				}
				TradeSetupTO tradeSetup2 = validateAndSetDetails(strikes, stock, "c2");
				buildTradeSetupTO(tradeSetup2, properties, values, stock);
				if (isTradeProcessed(tradeSetup2)) {
					tradeSetup2.setStrikes(strikes);
					trades.add(tradeSetup2);
				}
				TradeSetupTO tradeSetup3 = validateAndSetDetails(strikes, stock, "c3");
				buildTradeSetupTO(tradeSetup3, properties, values, stock);
				if (isTradeProcessed(tradeSetup3)) {
					tradeSetup3.setStrikes(strikes);
					trades.add(tradeSetup3);
				}
				TradeSetupTO tradeSetup4 = validateAndSetDetailsBasedOnVolume(strikes, stock, "volume");
				buildTradeSetupTO(tradeSetup4, properties, values, stock);
				if (isTradeProcessed(tradeSetup4)) {
					tradeSetup4.setStrikes(strikes);
					trades.add(tradeSetup4);
				}
			}
		}
		return trades;
	}

	private List<String> getStockType(MarketMoverData marketMoverData, String type) {
		if (marketMoverData.getInOldOi() == null || marketMoverData.getInNewOi() == null ||
				marketMoverData.getInOldClose() == null || marketMoverData.getInNewClose() == null) {
			log.error("OI or Close data is missing for stock: {}", marketMoverData.getStSymbolName());
			return null;
		}
		if (sectorList.contains(marketMoverData.getStSymbolName())) {
			log.info("Skipping sector stock: {}", marketMoverData.getStSymbolName());
			return null;
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
		log.info("Stock {} with OI Change {}", marketMoverData.getStSymbolName(), oiChg);
		if ("G".equals(type) && (oiChg > 2 || oiChg < -2) && lptChgPer > 2) {
			Arrays.asList("positive", oiChg + "", lptChgPer + "", oiInterpretation);
		} else if ("L".equals(type) && oiChg < -1) {
			Arrays.asList("negative", oiChg + "", lptChgPer + "", oiInterpretation);
			return null;
		} else {
			log.info("Stock {} Oi Change Per {} Ltp change per {}", marketMoverData.getStSymbolName(), oiChg, lptChgPer);
			return null;
		}
		return Arrays.asList("positive", oiChg + "", lptChgPer + "", oiInterpretation);

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

		StrikeTO peVolumeStrike = commonUtils.getLargestPeVolumeStrike(strikes);
		StrikeTO ceVolumeStrike = commonUtils.getLargestCeVolumeStrike(strikes);
		boolean allValid = isValidStrike(strikeUp3) && isValidStrike(strikeUp2) && isValidStrike(strikeUp1) &&
				isValidStrike(strikeDown1) && isValidStrike(strikeDown2) && isValidStrike(strikeDown3);
		TradeSetupTO tradeSetup = new TradeSetupTO();
		if (("c3".equals(criteria) || "c5".equals(criteria))) {
			if (((strike0.getPeOiChg() > ((strike0.getCeOiChg()) * 0.7)) &&
					(("c3".equals(criteria) && allValid && (strikeUp1.getCeOiChg() < 0 && strikeUp2.getCeOiChg() < 0)) ||
							("c5".equals(criteria) && (strikeUp1.getCeOiChg() <= 0 && strikeUp2.getCeOiChg() < 0))) &&
					(strikeDown1.getPeOiChg() + strikeDown2.getPeOiChg() + strikeDown3.getPeOiChg()) > 0)) {
				tradeSetup.setStrategy(criteria);
				double val = (strike0.getStrikePrice() + strike0.getCurPrice()) / 2;
				if (strike0.getCurPrice() < 500) {
					tradeSetup.setEntry1(strike0.getStrikePrice());
					tradeSetup.setEntry2((strike0.getStrikePrice() + strikeUp1.getStrikePrice()) / 2);
				} else {
					if (val > strike0.getCurPrice()) {
						tradeSetup.setEntry1(strike0.getCurPrice());
						tradeSetup.setEntry2(val);
					} else {
						tradeSetup.setEntry1(val);
						tradeSetup.setEntry2(strike0.getStrikePrice());
					}
				}
				setTargetPrices(tradeSetup, strikes);
				tradeSetup.setStopLoss1(strikeUp2.getStrikePrice());

				if (tradeSetup.getTarget1() <= strikes.get(0).getStrikePrice() || peVolumeStrike.getStrikePrice() < strikes.get(-1).getStrikePrice()) {
					return null;
				}
				return tradeSetup;
			}
		} else if ((criteria.equals("c2") || "c6".equals(criteria)) && isValidStrike(strike0) && allValid
				&& strike0.getCeOiChg() < 0 && strike0.getPeOiChg() > 0 &&
				strikeUp1.getCeOiChg() < 0 && strikeUp2.getCeOiChg() < 0 &&
				strikeDown1.getPeOiChg() > 0 && strikeDown2.getPeOiChg() > 0) {

			tradeSetup.setEntry1((strike0.getStrikePrice() + strikeDown1.getStrikePrice()) / 2);
			tradeSetup.setEntry2(strike0.getStrikePrice());
			tradeSetup.setTarget2((strikeDown2.getStrikePrice() + strikeDown3.getStrikePrice()) / 2);
			tradeSetup.setTarget1(strikeDown2.getStrikePrice());
			tradeSetup.setStopLoss1(strikeUp2.getStrikePrice());
			tradeSetup.setStrategy(criteria);
			return tradeSetup;
		} else if (criteria.equals("c1") && isValidStrike(strike0) && allValid &&
				strikeUp1.getCeOiChg() < 0 && strikeUp2.getCeOiChg() < 0 && strikeUp3.getCeOiChg() < 0 &&
				strikeDown1.getPeOiChg() > 0 && strikeDown2.getPeOiChg() > 0 && strikeDown3.getPeOiChg() > 0) {
			tradeSetup.setEntry1((strike0.getStrikePrice() + strikeDown1.getStrikePrice()) / 2);
			tradeSetup.setEntry2(strike0.getStrikePrice());
			tradeSetup.setTarget1((strikeDown2.getStrikePrice() + strikeDown3.getStrikePrice()) / 2);
			tradeSetup.setTarget2(strikeDown3.getStrikePrice());
			tradeSetup.setStopLoss1(strikeUp2.getStrikePrice());
			tradeSetup.setStrategy(criteria);
			return tradeSetup;
		} else if (criteria.equals("c4") && isValidStrike(strike0) && allValid &&
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

	private TradeSetupTO validateAndSetDetailsBasedOnVolume(Map<Integer, StrikeTO> strikes, String stock, String criteria) {
		if (strikes == null || strikes.isEmpty()) {
			log.error("No strikes found for stock: {}", stock);
			return null;
		}

		if (strikes != null && !strikes.isEmpty() && strikes.get(1) != null && strikes.get(-1) != null) {
			int highCEVolumeAt = getHighVolumeCeStrikeIndex(strikes);
			int highPEVolumeAt = getHighVolumePeStrikeIndex(strikes);
			int ceVolume = getTotalCeVolume(strikes);
			int peVolume = getTotalPeVolume(strikes);
			StrikeTO highCeVolumeStrike = commonUtils.getLargestCeVolumeStrike(strikes);
			StrikeTO highPeVolumeStrike = commonUtils.getLargestPeVolumeStrike(strikes);

			if (ceVolume > (3 * peVolume) && highCeVolumeStrike.getCeVolume() > (2 * highPeVolumeStrike.getPeVolume())) {
				TradeSetupTO tradeSetup = new TradeSetupTO();
				tradeSetup.setStockSymbol(stock);
				tradeSetup.setEntry1((strikes.get(0).getStrikePrice() + strikes.get(1).getStrikePrice()) / 2);
				tradeSetup.setTarget1(strikes.get(2).getStrikePrice());
				tradeSetup.setTarget2(strikes.get(3).getStrikePrice());
				tradeSetup.setStrategy(criteria);
				double stopLoss = Math.min(highPeVolumeStrike.getStrikePrice(), (strikes.get(-1).getStrikePrice() + strikes.get(-2).getStrikePrice()) / 2);
				tradeSetup.setStopLoss1(stopLoss);
				tradeSetup.setStrikes(strikes);
				return tradeSetup;
			}
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
		StrikeTO ceVolumeStrike = commonUtils.getLargestCeVolumeStrike(strikes);
		Double target2Price = null;
		if (strikes.get(1) != null && strikes.get(1).getPeOiChg() < 0) {
			target2Price = strikes.get(0) != null ? strikes.get(0).getStrikePrice() : null;
		} else if (strikes.get(2) != null && strikes.get(2).getPeOiChg() < 0) {
			target2Price = strikes.get(1) != null ? strikes.get(1).getStrikePrice() : null;
		} else if (strikes.get(3) != null && strikes.get(3).getPeOiChg() < 0) {
			target2Price = strikes.get(2) != null ? strikes.get(2).getStrikePrice() : null;
		} else if (strikes.get(4) != null && strikes.get(4).getPeOiChg() < 0) {
			target2Price = strikes.get(3) != null ? strikes.get(3).getStrikePrice() : null;
		} else {
			target2Price = strikes.get(4) != null ? strikes.get(4).getStrikePrice() : null;
		}
		double target1Price = ceVolumeStrike.getStrikePrice();
		if (target2Price != null) {
			if (target2Price < target1Price) {
				if (target2Price < 500) {
					tradeSetupTO.setTarget1(target2Price - 2);
				} else {
					tradeSetupTO.setTarget1(target2Price);
				}
				if (target2Price < 500) {
					tradeSetupTO.setTarget2(target1Price - 2);
				} else {
					tradeSetupTO.setTarget2(target1Price);
				}
			} else {
				if (target2Price < 500) {
					tradeSetupTO.setTarget2(target2Price - 2);
				} else {
					tradeSetupTO.setTarget2(target2Price);
				}
				if (target1Price < 500) {
					tradeSetupTO.setTarget1(target1Price - 2);
				} else {
					tradeSetupTO.setTarget1(target1Price);
				}
			}
		} else {
			if (target1Price < 500) {
				tradeSetupTO.setTarget1(target1Price - 2);
			} else {
				tradeSetupTO.setTarget1(target1Price);
			}
		}
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

	private boolean isValidStrike(StrikeTO strike, boolean up) {
		if (strike == null) {
			return false;
		}
		String ceOiInt = strike.getCeOiInt();
		String peOiInt = strike.getPeOiInt();
		if (up) {
			return ("SC".equals(ceOiInt) || "LBU".equals(ceOiInt));
		} else {
			return ("LU".equals(peOiInt) || "SBU".equals(peOiInt));
		}
	}

	private void buildTradeSetupTO(TradeSetupTO tradeSetupTO, Properties prop, List<String> values, String stock) {
		if (tradeSetupTO != null) {
			tradeSetupTO.setStockSymbol(stock);
			tradeSetupTO.setFetchTime(FormatUtil.getTimeHHmmss(prop.getEndTime()).format(DateTimeFormatter.ofPattern("HH:mm")));
			tradeSetupTO.setStockDate(prop.getStockDate());
			if (values != null) {
				if (values.size() > 1) {
					tradeSetupTO.setOiChgPer(Double.valueOf(values.get(1)));
				}
				if (values.size() > 2) {
					tradeSetupTO.setLtpChgPer(Double.valueOf(values.get(2)));
				}
				if (values.size() > 1) {
					tradeSetupTO.setType(values.get(0));
				}
			}
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

	public List<TradeSetupTO> testPositiveMarketMovers(Properties properties) {
		List<TradeSetupTO> list = tradeSetupManager.findTradeSetupByDate(properties.getStockDate());
		for (TradeSetupTO trade : list) {
			if (properties.getStrategy() != null && !properties.getStrategy().isEmpty() && !trade.getStrategy().equals(properties.getStrategy())) {
				continue;
			}
			if (StringUtils.hasLength(properties.getStockName()) && !trade.getStockSymbol().equals(properties.getStockName())) {
				continue;
			}
			try {
				String date = trade.getStockDate();

				int noOfDays = 20;
				int entryDays = 1;
				int processedDays = 0;
				boolean isEntry1 = StringUtils.hasLength(trade.getEntry1Time());
				boolean isEntry2 = StringUtils.hasLength(trade.getEntry2Time()) || trade.getEntry2() == null;
				boolean isTarget1 = StringUtils.hasLength(trade.getTarget1Time());
				boolean isTarget2 = StringUtils.hasLength(trade.getTarget2Time()) || trade.getTarget2() == null;
				boolean isStopLoss1 = StringUtils.hasLength(trade.getStopLoss1Time());
				for (int i = 1; i < noOfDays; i++) {
					if (!isEntry1 && !isEntry2 && processedDays >= entryDays) {
						trade.setTradeNotes("N-E (2D)");
						trade.setStatus("N-E");
						break;
					}
					if ((isTarget1 && isTarget2) || isStopLoss1) {
						break;
					}
					if (i > 0) {
						date = FormatUtil.addDays(properties.getStockDate(), i);
					}
					if (MarketHolidayUtils.isWeekend(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")))) {
						continue; // Skip non-trading days
					}
					String time = "09:15";
					if (i == 0) {
						time = trade.getFetchTime();
					}

//					List<HistoricalQuote> candles = getHistoricalQuotes(trade.getStockSymbol(), date, time, properties.getInterval(), "oi");
					List<Candle> candles = getHistoricalQuotes(trade.getStockSymbol(), date, time, properties.getInterval(), "oi");
					processedDays++;
					if (candles != null && candles.size() > 1) {
						candles = candles.subList(0, candles.size() - 1);
						for (int j = 0; j < candles.size(); j++) {
							Candle candle = candles.get(j);
							String arr[] = trade.getFetchTime().split(":");
							if ((i == 0 && candle.getEndTime().isAfter(LocalTime.of(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]), 0))) || i > 0) {
								if (candle.getHigh() <= 0 || candle.getLow() <= 0) {
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
								double candleLow = candle.getLow();
								double candleHigh = candle.getHigh();
								String dateTime = candle.getDate() + " " + candle.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
								if (!isEntry1 && candleLow <= trade.getEntry1()) {
									trade.setEntry1Time(dateTime);
									trade.setTradeNotes(trade.getTradeNotes() != null ? (trade.getTradeNotes() + "E1 ") : "E1 ");
									trade.setStatus("O");
									isEntry1 = true;
								}
								if (!isEntry1 && !isEntry2 && trade.getEntry2() != null && candleLow <= trade.getEntry2()) {
									trade.setEntry2Time(dateTime);
									trade.setTradeNotes(trade.getTradeNotes() + "E2 ");
									trade.setStatus("O");
									isEntry2 = true;
								}
								if ((isEntry1 || isEntry2) && !isTarget1 && trade.getTarget1() != null && candleHigh >= trade.getTarget1()) {
									trade.setTarget1Time(dateTime);
									trade.setTradeNotes(trade.getTradeNotes() + "T1 ");
									trade.setStatus("P");
									isTarget1 = true;
								}
								if ((isEntry1 || isEntry2) && !isTarget2 && trade.getTarget2() != null && candleHigh >= trade.getTarget2()) {
									trade.setTarget2Time(dateTime);
									trade.setTradeNotes(trade.getTradeNotes() + "T2 ");
									trade.setStatus("P");
									isTarget2 = true;
								}
								if ((isEntry1 || isEntry2) && (!isTarget1 || !isTarget2) && candleLow <= trade.getStopLoss1()) {
									trade.setStopLoss1Time(dateTime);
									if (!trade.getTradeNotes().contains("SL ")) {
										trade.setTradeNotes(trade.getTradeNotes() + "SL ");
									}
									if (isTarget1) {
										trade.setStatus("P");
									} else {
										trade.setStatus("L");
									}
//								isStopLoss1 = true;
								}
							}
						}
					}
				}
			} catch (Exception e) {
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

	private List<Candle> getHistoricalQuotes(String stockSymbol, String date, String time, int interval, String type) throws IOException {
		try {
			if ("oi".equalsIgnoreCase(type)) {
				Properties properties = new Properties();
				properties.setStockName(stockSymbol);
				properties.setInterval(interval);
				LocalTime localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
				localTime.plusMinutes(1);
				List<Candle> candles = commonValidation.getCandles(properties, date + " " + localTime.format(DateTimeFormatter.ofPattern("HH:mm")), date + " " + "15:15");
				Collections.reverse(candles);
				return candles;
			}
		} catch (Exception e) {
			log.error("Error fetching historical quotes for stock {} {} : {}", stockSymbol, date, e.getMessage(), e);
		}
		return new ArrayList<>();
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
						StrikeTO highCeVolumeStrike = commonUtils.getLargestCeVolumeStrike(strikes);
						StrikeTO highPeVolumeStrike = commonUtils.getLargestPeVolumeStrike(strikes);
						/*if ( ceVolume > (3 * peVolume) ) {
							System.out.println("Stock " + stock + " time at " + endTime + "CE Volume: " + ceVolume + ", PE Volume: " + peVolume);
						}*/

						if (ceVolume > (3 * peVolume) && highCeVolumeStrike.getCeVolume() > (2 * highPeVolumeStrike.getPeVolume())) {
							System.out.println("Stock " + stock + " time at " + endTime + "CE Volume: " + ceVolume + ", PE Volume: " + peVolume + " High CE Strike: " + highCeVolumeStrike.getStrikePrice() + " High PE Strike: " + highPeVolumeStrike.getStrikePrice());
							TradeSetupTO tradeSetup = new TradeSetupTO();
							tradeSetup.setStockDate(properties.getStockDate());
							tradeSetup.setFetchTime(properties.getEndTime());
							tradeSetup.setStockSymbol(stock);
							tradeSetup.setEntry1((strikes.get(0).getStrikePrice() + strikes.get(1).getStrikePrice()) / 2);
							tradeSetup.setTarget1(strikes.get(2).getStrikePrice());
							tradeSetup.setTarget2(strikes.get(3).getStrikePrice());
							tradeSetup.setStrategy("VolumeBased");
							double stopLoss = Math.min(highPeVolumeStrike.getStrikePrice(), (strikes.get(-1).getStrikePrice() + strikes.get(-2).getStrikePrice()) / 2);
							tradeSetup.setStopLoss1(stopLoss);
							tradeSetup.setStrikes(strikes);
							trades.add(tradeSetup);
							break;
						}
					}
				}
			} else {
				threadSleep(100);
				properties.setEndTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
				Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, marketMoverData.getStSymbolName());
				if (strikes != null && !strikes.isEmpty() && strikes.get(1) != null && strikes.get(-1) != null) {
					int highCEVolumeAt = getHighVolumeCeStrikeIndex(strikes);
					int highPEVolumeAt = getHighVolumePeStrikeIndex(strikes);
					int ceVolume = getTotalCeVolume(strikes);
					int peVolume = getTotalPeVolume(strikes);
					StrikeTO highCeVolumeStrike = commonUtils.getLargestCeVolumeStrike(strikes);
					StrikeTO highPeVolumeStrike = commonUtils.getLargestPeVolumeStrike(strikes);

					if (ceVolume > (3 * peVolume) && highCeVolumeStrike.getCeVolume() > (2 * highPeVolumeStrike.getPeVolume())) {
						TradeSetupTO tradeSetup = new TradeSetupTO();
						tradeSetup.setStockDate(properties.getStockDate());
						tradeSetup.setFetchTime(properties.getEndTime());
						tradeSetup.setStockSymbol(stock);
						tradeSetup.setEntry1((strikes.get(0).getStrikePrice() + strikes.get(1).getStrikePrice()) / 2);
						tradeSetup.setTarget1(strikes.get(2).getStrikePrice());
						tradeSetup.setTarget2(strikes.get(3).getStrikePrice());
						tradeSetup.setStrategy("VolumeBased");
						double stopLoss = Math.min(highPeVolumeStrike.getStrikePrice(), (strikes.get(-1).getStrikePrice() + strikes.get(-2).getStrikePrice()) / 2);
						tradeSetup.setStopLoss1(stopLoss);
						tradeSetup.setStrikes(strikes);
						trades.add(tradeSetup);
						break;
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


	public List<TradeSetupTO> optionChainData(Properties properties) {

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
			if (marketMoverData.getInNewDayHigh() < 150) {
				continue;
			}
			if (sectorList.contains(stock) || stock.contains("NIFTY")) {
				log.info("Skipping sector stock: {}", stock);
				continue;
			}

			log.info("Stock {} ", stock);
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
				threadSleep(500);
				LocalTime endTime1 = FormatUtil.getTimeHHmmss("13:00:00");
				LocalTime endTime = FormatUtil.getTime(startTime, interval);
				boolean isCriteria5Met = false;
				for (int i = interval; endTime.isBefore(endTime1); i += interval) {
					endTime = FormatUtil.getTime(startTime, i);
					if (properties.getStockDate().equals(LocalDate.now().toString()) && endTime.isAfter(LocalTime.now())) {
						break;
					}
					properties.setEndTime(endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
					Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, marketMoverData.getStSymbolName());
					if (strikes != null && !strikes.isEmpty()) {
						double curPrice = strikes.get(0).getCurPrice();
						double openPrice = strikes.get(0).getOpenPrice();
						double ltpChgPer = (curPrice - openPrice) / openPrice * 100;
						if (ltpChgPer <= 0) {
							continue;
						}
						if (!isCriteria5Met) {
							TradeSetupTO tradeSetup1 = validateAndSetDetails(strikes, marketMoverData.getStSymbolName(), "c5");
							List<String> values = Arrays.asList("P", null, ltpChgPer + "");
							buildTradeSetupTO(tradeSetup1, properties, values, marketMoverData.getStSymbolName());
							if (isTradeProcessed(tradeSetup1)) {
								boolean isTargetReached = isTargetReached(tradeSetup1);
								if (isTargetReached) {
									tradeSetup1.setTradeNotes("TR");
									log.info("stock {} target already reached", marketMoverData.getStSymbolName());
								} else {
									tradeSetup1.setStrikes(strikes);
									trades.add(tradeSetup1);
									log.info("stock {} criteria5 met", marketMoverData.getStSymbolName());
									isCriteria5Met = true;
								}
							}
						}
						if (isCriteria5Met) {
							break;
						}
					}
				}
			} else {
				threadSleep(100);
				properties.setEndTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
				Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, marketMoverData.getStSymbolName());
				if (strikes != null && !strikes.isEmpty()) {
					double curPrice = strikes.get(0).getCurPrice();
					double openPrice = strikes.get(0).getOpenPrice();
					double ltpChgPer = (curPrice - openPrice) / openPrice * 100;
					TradeSetupTO tradeSetup1 = validateAndSetDetails(strikes, marketMoverData.getStSymbolName(), "c5");
					List<String> values = Arrays.asList("P", null, ltpChgPer + "");
					buildTradeSetupTO(tradeSetup1, properties, values, marketMoverData.getStSymbolName());
					if (ltpChgPer > 0 && isTradeProcessed(tradeSetup1)) {
						tradeSetup1.setStrikes(strikes);
						trades.add(tradeSetup1);
					}
				}
			}
		}
		persistTrades(trades);
		if (properties.getStrategy() != null && !properties.getStrategy().isEmpty()) {
			return trades.stream()
					.filter(tradeSetupTO -> {
						if (tradeSetupTO.getStrategy() == null) {
							return false;
						}
						return tradeSetupTO.getStrategy().equals(properties.getStrategy());
					}).collect(Collectors.toList());
		}
		return trades;
	}

	private boolean isTargetReached(TradeSetupTO trade) {
		try {
			String date = trade.getStockDate();
			String time = trade.getFetchTime();
			Calendar from = CalendarUtil.buildCalendar(date, "09:15");
			Calendar to = CalendarUtil.buildCalendar(date, time);
			List<HistoricalQuote> candles = yahooFinanceService.getHistoricalQuote(trade.getStockSymbol(), from, to, 2 + "");
			for (HistoricalQuote hq : candles) {
				if (hq.getHigh() != null && trade.getTarget1() != null) {
					double high = hq.getHigh().doubleValue();
					double target = trade.getTarget1();
					if (high >= target * 0.998) {
						return true;
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return false;
	}
}
