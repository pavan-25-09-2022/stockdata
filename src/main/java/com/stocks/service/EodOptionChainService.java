package com.stocks.service;

import com.stocks.dto.EodAnalyzerTO;
import com.stocks.dto.FutureEodAnalyzer;
import com.stocks.dto.Properties;
import com.stocks.dto.StockEODResponse;
import com.stocks.dto.StrikeTO;
import com.stocks.dto.TradeSetupTO;
import com.stocks.repository.EodAnalyzerManager;
import com.stocks.repository.TradeSetupManager;
import com.stocks.utils.FormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EodOptionChainService {

	@Autowired
	private CommonValidation commonValidation;
	@Autowired
	private IOPulseService ioPulseService;
	@Autowired
	private MarketMovers marketMovers;
	@Autowired
	private CalculateOptionChain calculateOptionChain;
	@Autowired
	private TradeSetupManager tradeSetupManager; 
	@Autowired
	private EodAnalyzerManager eodAnalyzerManager;

	public List<TradeSetupTO> validateEodOptionChain(Properties properties) {
		LocalDate date = LocalDate.parse(properties.getStockDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
		List<TradeSetupTO> eodOptionChain = new ArrayList<>();
		if (isWeekend) {
			log.info("Eod Option Chain is a weekend");
			return eodOptionChain;
		}
		properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));

		Set<String> stockList = getStockList(properties);
		if (stockList.isEmpty()) {
			log.error("No stocks available for processing");
			return eodOptionChain;
		}
		Map<String, String> validStocks = preFilterStocksByEod(stockList, properties.getStockDate());
		if (validStocks.isEmpty()) {
			log.info("No valid stocks found after EOD pre-filtering");
			return eodOptionChain;
		}

		for (Map.Entry<String, String> entry : validStocks.entrySet()) {
			properties.setStockName(entry.getKey());
			log.info("Eod Option Chain validation for stock {}: ", entry.getKey());
			TradeSetupTO tradeSetupTO = processStock(properties);
			if (tradeSetupTO != null) {
				tradeSetupTO.setTradeNotes(entry.getValue());
				eodOptionChain.add(tradeSetupTO);
				tradeSetupManager.saveTradeSetup(tradeSetupTO);
			}
		}

		return eodOptionChain;

	}

	private TradeSetupTO processStock(Properties properties) {
		try {
			properties.setStartTime("09:16:00");
			if ("test".equalsIgnoreCase(properties.getEnv())) {
				LocalTime endTime = FormatUtil.getTimeHHmmss("09:30:00");

				while (true) {
					assert endTime != null;
					if ((LocalDate.now().toString().equals(properties.getStockDate()) && endTime.isAfter(LocalTime.now())) ||
							endTime.isAfter(LocalTime.of(14, 0))) {
						break;
					}
					properties.setEndTime(FormatUtil.formatTimeHHmmss(endTime));
					// Get strikes data
					Map<Integer, StrikeTO> strikes = calculateOptionChain.getStrikes(properties, properties.getStockName());
					TradeSetupTO c7TradeSetup = marketMovers.validateAndSetDetails(strikes, properties, "c7");
					// Create base trade setup
					if (c7TradeSetup != null) {
						TradeSetupTO tradeSetupTO = createBaseTradeSetup(properties);
						tradeSetupTO.setEntry1(c7TradeSetup.getEntry1());
						tradeSetupTO.setStopLoss1(strikes.get(-1).getStrikePrice());
						tradeSetupTO.setEntry2(strikes.get(0).getCurPrice());
						tradeSetupTO.setTarget1(c7TradeSetup.getTarget1());
						tradeSetupTO.setTarget2(c7TradeSetup.getTarget2());
						return tradeSetupTO;
					}
					// Increment endTime by 15 minutes for next iteration
					endTime = endTime.plusMinutes(properties.getInterval());
				}
			}

		} catch (Exception e) {
			log.error("Error processing stock {}: {}", properties.getStockName(), e.getMessage());
			return null;
		}
		return null;
	}

	private TradeSetupTO createBaseTradeSetup(Properties properties) {
		TradeSetupTO tradeSetupTO = new TradeSetupTO();
		tradeSetupTO.setStockSymbol(properties.getStockName());
		tradeSetupTO.setStockDate(properties.getStockDate());
		tradeSetupTO.setFetchTime(properties.getEndTime().substring(0, 5));
		tradeSetupTO.setType("P");
		tradeSetupTO.setStrategy("C7");
		return tradeSetupTO;
	}

	private Set<String> getStockList(Properties properties) {
		try {
			if (properties.getStockName() != null && !properties.getStockName().trim().isEmpty()) {
				return Arrays.stream(properties.getStockName().split(","))
						.map(String::trim)
						.filter(stock -> !stock.isEmpty())
						.collect(Collectors.toSet());
			} else {
				Set<String> availableStocks = ioPulseService.getAvailableStocks();
				return availableStocks != null ? availableStocks : new HashSet<>();
			}
		} catch (Exception e) {
			log.error("Error fetching available stocks", e);
			return new HashSet<>();
		}
	}

	private Map<String, String> preFilterStocksByEod(Set<String> stockList, String stockDate) {
		Map<String, String> validStocks = new HashMap<>();
		for (String stock : stockList) {
			try {
				String eodResponse = processEodResponse(stock, stockDate);
				if (eodResponse != null && !eodResponse.isEmpty()) {
					String ystEod = eodResponse.split("-")[0];
					String yst2Eod = eodResponse.split("-")[1];
					if (("LBU".equals(ystEod)) && ("LBU".equals(yst2Eod) || "SC".equals(yst2Eod))) {
						validStocks.put(stock, eodResponse);
					}
				}
			} catch (Exception e) {
				log.error("Error processing EOD for stock {}: {}", stock, e.getMessage());
			}
		}
		return validStocks;
	}

	private String processEodResponse(String stock, String stockDate) {
		// List<EodAnalyzerTO> eodAnalyzers = eodAnalyzerManager.fetchStockAndStockDate(stock);
		// if (eodAnalyzers.isEmpty()) {
		// 	return null;
		// }
		// for(int i=0; i<eodAnalyzers.size(); i++) {
		// 	if(stockDate.equals(eodAnalyzers.get(i).getStockDate())) {
		// 		return eodAnalyzers.get(i).getOiInterpretation();
		// 	}
		// }
		// return null;
	
		StockEODResponse eod = ioPulseService.getMonthlyData(stock);
		if (eod == null || eod.getData().size() < 3) {
			log.info("EOD data is insufficient for stock: {}", stockDate);
			return null;
		}
		FutureEodAnalyzer data = eod.getData().get(0);
		if (data.getInDayHigh() < 200) {
			return null;
		}
		// Extract data for the last three days
		for (int i = 0; i < eod.getData().size(); i++) {
			FutureEodAnalyzer futureEodAnalyzer = eod.getData().get(i);
			if (stockDate.equals(futureEodAnalyzer.getStFetchDate())) {
				FutureEodAnalyzer dayM1 = eod.getData().get(i + 1);
				FutureEodAnalyzer dayM2 = eod.getData().get(i + 2);
				FutureEodAnalyzer dayM3 = eod.getData().get(i + 3);
				String oi1 = calculateOiInterpretation(dayM1, dayM2);
				String oi2 = calculateOiInterpretation(dayM2, dayM3);
				return (oi1 + "-" + oi2);
			}
		}
		return null;
	}

	private String calculateOiInterpretation(FutureEodAnalyzer current, FutureEodAnalyzer previous) {
		double ltpChange = Double.parseDouble(String.format("%.2f", current.getInClose() - previous.getInClose()));
		long oiChange = Long.parseLong(current.getInOi()) - Long.parseLong(previous.getInOi());
		return (oiChange > 0)
				? (ltpChange > 0 ? "LBU" : "SBU")
				: (ltpChange > 0 ? "SC" : "LU");
	}
}
