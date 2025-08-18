package com.stocks.service;

import com.stocks.dto.FutureAnalysis;
import com.stocks.dto.FutureEodAnalyzer;
import com.stocks.dto.MarketMoverData;
import com.stocks.dto.MarketMoversResponse;
import com.stocks.dto.Properties;
import com.stocks.dto.StockEODResponse;
import com.stocks.utils.DateUtil;
import com.stocks.utils.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FutureEodAnalyzerService {

	@Autowired
	private IOPulseService ioPulseService;

	@Autowired
	private MailService mailService;

	@Autowired
	FutureAnalysisService futureAnalysisService;

	@Autowired
	TrendLineService trendLineService;

	@Autowired
	YahooFinanceService yahooFinanceService;

	@Autowired
	private OptionChainService optionCHainService;

	@Autowired
	private CalculateOptionChain calculateOptionChain;

	@Autowired
	private FutureAnalysisManager futureAnalysisManager;

	@Value("${calculateEodValue}")
	private int calculateEodValue;

	private static final Logger log = LoggerFactory.getLogger(FutureEodAnalyzerService.class);

	public String processEodResponse(Properties properties) {
		// Path to the file containing the stock list
		String filePath = "src/main/resources/stocksList.txt";

		// Read all lines from the file into a List
		List<String> stockList;
		if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
			stockList = Arrays.asList(properties.getStockName().split(","));
		} else {
			try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
				stockList = lines.collect(Collectors.toList());
			} catch (IOException e) {
				log.error("Error reading file: " + e.getMessage());
				return "No Records";
			}
		}


		List<String> longunwindingFollowedByShortCovering = new ArrayList<>();
		longunwindingFollowedByShortCovering.add("Eod Analyser");
		longunwindingFollowedByShortCovering.add("\n");
		longunwindingFollowedByShortCovering.add("... Long unwinding followed by short covering.....");

		List<String> shortCoveringFollowedByLongBuildUp = new ArrayList<>();
		shortCoveringFollowedByLongBuildUp.add("Eod Analyser");
		shortCoveringFollowedByLongBuildUp.add("\n");
		shortCoveringFollowedByLongBuildUp.add("... Short Covering followed by Long Build Up.....");

		List<String> shortCoveringFollowedByLongUnwinding = new ArrayList<>();
		shortCoveringFollowedByLongUnwinding.add("Eod Analyser");
		shortCoveringFollowedByLongUnwinding.add("\n");
		shortCoveringFollowedByLongUnwinding.add("... Short Covering followed by LongUnWinding.....");

		List<String> longUnwindingFollowedByShortBuildUp = new ArrayList<>();
		longUnwindingFollowedByShortBuildUp.add("Eod Analyser");
		longUnwindingFollowedByShortBuildUp.add("\n");
		longUnwindingFollowedByShortBuildUp.add("... Long Unwinding followed by short Build Up.....");

		List<String> positiveStocks = new ArrayList<>();
		List<String> negativeStocks = new ArrayList<>();

		boolean isLongunwindingFollowedByShortCovering = false;
		boolean isShortCoveringFollowedByLongBuildUp = false;
		boolean isShortCoveringFollowedByLongUnwinding = false;
		boolean isLongUnwindingFollowedByShortBuildUp = false;

		for (String stock : stockList) {

			StockEODResponse eod = ioPulseService.getMonthlyData(stock);
			if (eod == null || eod.getData() == null || eod.getData().size() < 3) {
				log.info("EOD data is insufficient for stock: {}", stock);
				continue;
			}

			// Calculate
			if (eod.getData().size() > calculateEodValue) {
				for (int i = 0; i < calculateEodValue; i++) {
					calculateOiInterpretation(eod.getData().get(i), eod.getData().get(i + 1));
				}

				for (int i = calculateEodValue - 1; i > 0; i--) {

					FutureEodAnalyzer stockDetails = eod.getData().get(i);
					FutureEodAnalyzer nextDayStockDetails = eod.getData().get(i - 1);

					if (("LU".equals(stockDetails.getOiInterpretation()) && "SC".equals(nextDayStockDetails.getOiInterpretation())) ||
							("SC".equals(stockDetails.getOiInterpretation()) && "LBU".equals(nextDayStockDetails.getOiInterpretation()))) {

						boolean highCondition = (nextDayStockDetails.getInDayHigh() > stockDetails.getInDayHigh());
						boolean closeCondition = (nextDayStockDetails.getInClose() > stockDetails.getInDayHigh());
						boolean strengthCondition = (nextDayStockDetails.getStrength() > stockDetails.getStrength());
						boolean nextdayLow = stockDetails.getInDayLow() < nextDayStockDetails.getInDayLow();

						if (highCondition && closeCondition && strengthCondition && nextdayLow) {

							if ("LU".equals(stockDetails.getOiInterpretation())) {
								longunwindingFollowedByShortCovering.add("\n");
								longunwindingFollowedByShortCovering.add("..... .." + stock + " ......with volume " + (nextDayStockDetails.getInVolume() > stockDetails.getInVolume()));
								isLongunwindingFollowedByShortCovering = true;

							} else {
								shortCoveringFollowedByLongBuildUp.add("\n");
								shortCoveringFollowedByLongBuildUp.add("..... .." + stock + " ......with volume " + (nextDayStockDetails.getInVolume() > stockDetails.getInVolume()));
								isShortCoveringFollowedByLongBuildUp = true;
							}
							positiveStocks.add(stock);
						}
					} else if (("SC".equals(stockDetails.getOiInterpretation()) && "LU".equals(nextDayStockDetails.getOiInterpretation()))
							|| ("LU".equals(stockDetails.getOiInterpretation()) && "SBU".equals(nextDayStockDetails.getOiInterpretation()))) {


						boolean negativeLowCondition = (nextDayStockDetails.getInDayLow() < stockDetails.getInDayLow());
						boolean negativeCloseCondition = (nextDayStockDetails.getInClose() < stockDetails.getInDayLow());
						boolean negativeStrengthCondition = (nextDayStockDetails.getStrength() > stockDetails.getStrength());
						boolean negativeHighCondition = (nextDayStockDetails.getInDayHigh() < stockDetails.getInDayHigh());

						if (negativeLowCondition && negativeCloseCondition && negativeStrengthCondition && negativeHighCondition) {
							if ("SC".equals(stockDetails.getOiInterpretation())) {
								shortCoveringFollowedByLongUnwinding.add("\n");
								shortCoveringFollowedByLongUnwinding.add("..... .." + stock + " ....... with volume " + (nextDayStockDetails.getInVolume() > stockDetails.getInVolume()));
								isShortCoveringFollowedByLongUnwinding = true;
							} else {
								longUnwindingFollowedByShortBuildUp.add("\n");
								longUnwindingFollowedByShortBuildUp.add("..... .." + stock + " ......with volume " + (nextDayStockDetails.getInVolume() > stockDetails.getInVolume()));
								isLongUnwindingFollowedByShortBuildUp = true;
							}
							negativeStocks.add(stock);
						}
					}
				}
			}
		}

		if (isLongUnwindingFollowedByShortBuildUp) {
			mailService.sendEmailList(longUnwindingFollowedByShortBuildUp, "Negative Stocks with LU followed SBU ");
		}

		if (isLongunwindingFollowedByShortCovering) {
			mailService.sendEmailList(longunwindingFollowedByShortCovering, "Positive Stocks with LU followed by SC");
		}

		if (isShortCoveringFollowedByLongBuildUp) {
			mailService.sendEmailList(shortCoveringFollowedByLongBuildUp, "Positive Stocks with SC followed by LBU");
		}

		if (isShortCoveringFollowedByLongUnwinding) {
			mailService.sendEmailList(shortCoveringFollowedByLongUnwinding, "Negative Stocks with SC followed by LU");
		}

		List<String> stocks = new ArrayList<>();
		stocks.addAll(positiveStocks);
		stocks.addAll(negativeStocks);

		properties.setStockName(String.join(",", stocks));
		Map<String, Map<String, FutureAnalysis>> stringMapMap = futureAnalysisService.futureAnalysis(properties);
		try {
			yahooFinanceService.combineFutureAndSpotDetails(properties, stringMapMap);
			System.out.println("---------------");
			for (Map.Entry<String, Map<String, FutureAnalysis>> map : stringMapMap.entrySet()) {

				String key = map.getKey();
				Map<String, FutureAnalysis> value = map.getValue();
				System.out.println("Key " + key);
				value.forEach((key1, value1) -> System.out.println("Duration " + key1 + " Value " + value1));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		List<String> emailContent = new ArrayList<>();
		for (String positive : positiveStocks) {
			Map<String, List<String>> trendLines = trendLineService.findTrendLines(stringMapMap.get(positive), true);
			trendLines.forEach((key1, value1) -> {
				emailContent.add("Postive Stock " + positive + " with time Frame" + properties.getInterval() + " for " + key1 + " Trend Lines " + value1.toString());
				emailContent.add("\n");
			});
		}
		for (String negative : negativeStocks) {
			Map<String, List<String>> trendLines2 = trendLineService.findTrendLines(stringMapMap.get(negative), false);
			trendLines2.forEach((key1, value1) -> {
				emailContent.add("Negative Stock " + negative + " with time Frame" + properties.getInterval() + " for " + key1 + " Trend Lines " + value1.toString());
				emailContent.add("\n");
			});
		}

		if (!emailContent.isEmpty()) {
			mailService.sendEmailList(emailContent, "Stocks with trend lines");
		}


		return stocks.toString();
	}

	private void calculateOiInterpretation(FutureEodAnalyzer current, FutureEodAnalyzer previous) {
		double ltpChange = Double.parseDouble(String.format("%.2f", current.getInClose() - previous.getInClose()));
		long oiChange = Long.parseLong(current.getInOi()) - Long.parseLong(previous.getInOi());
		String oiInterpretation = (oiChange > 0) ? (ltpChange > 0 ? "LBU" : "SBU") : (ltpChange > 0 ? "SC" : "LU");
		if (oiChange != 0) {
			DecimalFormat df = new DecimalFormat("#.###########");
			double percentageChangeInLTP = (ltpChange / previous.getInClose()) * 100;
			current.setPercentageChangeInLtp(percentageChangeInLTP);
			double percentageChangeInOI = ((double) oiChange / Long.parseLong(previous.getInOi())) * 100;
			current.setPercentageChangeInOi(percentageChangeInOI);
			double strength = Math.abs(Double.parseDouble(df.format(percentageChangeInLTP / percentageChangeInOI)));
			current.setStrength(strength);
		}
		current.setLtpChange(ltpChange);
		current.setOiChange(oiChange);
		current.setOiInterpretation(oiInterpretation);


	}

	public void getTrendLinesForNiftyAndBankNifty(Properties properties) {

		// Path to the file containing the stock list
		String filePath = "src/main/resources/sectorList.txt";

		// Read all lines from the file into a List
		List<String> stockList;
		if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
			stockList = Arrays.asList(properties.getStockName().split(","));
		} else {
			try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
				stockList = lines.collect(Collectors.toList());
				properties.setStockName(String.join(",", stockList));
			} catch (IOException e) {
				log.error("Error reading file: " + e.getMessage());
			}
		}

		// Read all lines from the file into a List

		Map<String, Map<String, FutureAnalysis>> stringMapMap = futureAnalysisService.futureAnalysis(properties);
		try {
			yahooFinanceService.combineFutureAndSpotDetails(properties, stringMapMap);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		List<String> emailContent = new ArrayList<>();
		for (Map.Entry<String, Map<String, FutureAnalysis>> stringMapEntry : stringMapMap.entrySet()) {
			String stock = stringMapEntry.getKey();
			Map<String, List<String>> trendLines = trendLineService.findTrendLines(stringMapMap.get(stock), true);
			trendLines.forEach((key1, value1) -> {
				emailContent.add("Postive  " + stock + " with time Frame" + properties.getInterval() + " for " + key1 + " Trend Lines " + value1.toString());
				emailContent.add("\n");
			});

			Map<String, List<String>> trendLines2 = trendLineService.findTrendLines(stringMapMap.get(stock), false);
			trendLines2.forEach((key1, value1) -> {
				emailContent.add("Negative " + stock + " with time Frame" + properties.getInterval() + " for " + key1 + " Trend Lines " + value1.toString());
				emailContent.add("\n");
			});
		}

		if (!emailContent.isEmpty()) {
			log.info("mail sent");
			mailService.sendEmailList(emailContent, "Sector with trend lines " + properties.getStockDate());
		}


	}


	public List<String> getOIChange(Properties properties) {

		// Path to the file containing the stock list
		String filePath = "src/main/resources/stocksList.txt";

		// Read all lines from the file into a List
		List<String> stockList;
		if (properties.getStockName() != null) {
			stockList = Arrays.asList(properties.getStockName().split(","));
		} else {
			try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
				stockList = lines.collect(Collectors.toList());
				properties.setStockName(String.join(",", stockList));
			} catch (IOException e) {
				log.error("Error reading file: " + e.getMessage());
				return Collections.EMPTY_LIST;
			}
		}

		Map<String, Map<String, FutureAnalysis>> stringMapMap = futureAnalysisService.futureAnalysis(properties);


		List<FutureAnalysis> result = new ArrayList<>();

		for (Map.Entry<String, Map<String, FutureAnalysis>> outerEntry : stringMapMap.entrySet()) {
			String outerKey = outerEntry.getKey();
			Map<String, FutureAnalysis> innerMap = outerEntry.getValue();
			if (!innerMap.isEmpty()) {
				FutureAnalysis firstInnerValue = innerMap.values().iterator().next();
				result.add(firstInnerValue);
			}
		}

		// List<FutureAnalysis> collect = result.stream().sorted(Comparator.comparing(FutureAnalysis::getPercentageChange).reversed()).collect(Collectors.toList());

		List<FutureAnalysis> positive = new ArrayList<>();
		List<FutureAnalysis> negative = new ArrayList<>();
		for (FutureAnalysis futureAnalysis : result) {
			if (futureAnalysis.getOiPercentageChange() > 1 && futureAnalysis.getInterpretation().equals("LBU") || futureAnalysis.getInterpretation().equals("SC")) {
				positive.add(futureAnalysis);
			}
			if (futureAnalysis.getInterpretation().equals("SBU") || futureAnalysis.getInterpretation().equals("LU")) {
				negative.add(futureAnalysis);
			}
		}

		System.out.println("Positive" + positive);
		System.out.println("Negative" + negative);

		List<String> conditionSatisfiedStocks = new ArrayList<>();
		for (FutureAnalysis futureAnalysis : positive) {

			Properties stockProperty = new Properties();
			stockProperty.setStockDate(properties.getStockDate());
			stockProperty.setInterval(properties.getInterval());
			stockProperty.setStockName(futureAnalysis.getSymbol());
			stockProperty.setStartTime("09:15:00");
			stockProperty.setExpiryDate("250626");
			boolean validStock = calculateOptionChain.isValidStock(futureAnalysis.getSymbol(), stockProperty.getStartTime(), stockProperty, true);
			if (validStock) {
				conditionSatisfiedStocks.add("Positive " + futureAnalysis.getSymbol() + " at " + futureAnalysis.getDuration());
			}

		}

		for (FutureAnalysis futureAnalysis : negative) {

			Properties stockProperty = new Properties();
			stockProperty.setStockDate(properties.getStockDate());
			stockProperty.setInterval(properties.getInterval());
			stockProperty.setStockName(futureAnalysis.getSymbol());
			stockProperty.setStartTime("09:15:00");
			stockProperty.setExpiryDate("250529");
			boolean validStock = calculateOptionChain.isValidStock(futureAnalysis.getSymbol(), stockProperty.getStartTime(), stockProperty, false);
			if (validStock) {
				conditionSatisfiedStocks.add("Negative " + futureAnalysis.getSymbol() + " at " + futureAnalysis.getDuration());
			}

		}


        /*List<String> positive = new ArrayList<>();
        List<String> negative = new ArrayList<>();
        for(Map.Entry<String, Map<String, FutureAnalysis>>  stringMapEntry : stringMapMap.entrySet()){
            String stock = stringMapEntry.getKey();
            Map<String, FutureAnalysis> value = stringMapEntry.getValue();
            for(Map.Entry<String, FutureAnalysis>  stockData : value.entrySet()){

                FutureAnalysis futureAnalysis = stockData.getValue();
                if(futureAnalysis.getPercentageChange() > 1 && futureAnalysis.getInterpretation().equals("LBU")){
                    positive.add(stock + " Positive percentage greater then one at " + futureAnalysis.getDuration());
                }
                if(futureAnalysis.getPercentageChange() > 1 && futureAnalysis.getInterpretation().equals("SBU")){
                    negative.add(stock + "Negative percentage greater then one at " + futureAnalysis.getDuration());
                }
            }
        }

        if (!positive.isEmpty()) {
            mailService.sendEmailList(positive, "Positive Percentage greater than one");
        }

        if (!negative.isEmpty()) {
            mailService.sendEmailList(negative, "Negative Percentage greater than one");
        }*/

		return conditionSatisfiedStocks;

	}


	public List<String> getOIChangeForSector(Properties properties) {

		// Path to the file containing the stock list
		String filePath = "src/main/resources/sectorList.txt";

		// Read all lines from the file into a List
		List<String> stockList;
		if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
			stockList = Arrays.asList(properties.getStockName().split(","));
		} else {
			try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
				stockList = lines.collect(Collectors.toList());
				properties.setStockName(String.join(",", stockList));
			} catch (IOException e) {
				log.error("Error reading file: " + e.getMessage());
				return Collections.EMPTY_LIST;
			}
		}

		Map<String, Map<String, FutureAnalysis>> stringMapMap = futureAnalysisService.futureAnalysis(properties);
		List<String> conditionSatisfiedStocks = new ArrayList<>();
		for (Map.Entry<String, Map<String, FutureAnalysis>> stringMapEntry : stringMapMap.entrySet()) {
			String stock = stringMapEntry.getKey();
			Map<String, FutureAnalysis> value = stringMapEntry.getValue();
			for (Map.Entry<String, FutureAnalysis> stockData : value.entrySet()) {
				FutureAnalysis futureAnalysis = stockData.getValue();
				Properties stockProperty = new Properties();
				stockProperty.setStockDate(properties.getStockDate());
				stockProperty.setInterval(properties.getInterval());
				stockProperty.setStockName(futureAnalysis.getSymbol());
				String startTime = futureAnalysis.getDuration().split("-")[0] + ":00";
				stockProperty.setStartTime(startTime);
				stockProperty.setExpiryDate("250626");
				//calculateOptionChain.changeInOI(futureAnalysis.getSymbol(), stockProperty.getStartTime(), stockProperty, true);
				if (futureAnalysis.getInterpretation().equals("LBU")) {
					boolean validStock = calculateOptionChain.changeInOI(futureAnalysis.getSymbol(), stockProperty.getStartTime(), stockProperty, true, futureAnalysis.getInterpretation());
					if (validStock) {
						conditionSatisfiedStocks.add("Positive " + futureAnalysis.getSymbol() + " at " + futureAnalysis.getDuration());
					}
				}
				if (futureAnalysis.getInterpretation().equals("SBU")) {
					boolean validStock = calculateOptionChain.changeInOI(futureAnalysis.getSymbol(), stockProperty.getStartTime(), stockProperty, false, futureAnalysis.getInterpretation());
					if (validStock) {
						conditionSatisfiedStocks.add("Negative " + futureAnalysis.getSymbol() + " at " + futureAnalysis.getDuration());
					}
				}
			}
		}

		return conditionSatisfiedStocks;

	}


	public String findDivergenceBasedOnOpenInterest(Properties properties) {

		// Path to the file containing the stock list
		String filePath = "src/main/resources/";

		if (properties.getFileName() != null && !properties.getFileName().isEmpty()) {
			filePath = filePath + properties.getFileName() + ".txt";
		} else {
			filePath = filePath + "sectorList.txt";
		}

		// Read all lines from the file into a List
		List<String> stockList;
		if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
			stockList = Arrays.asList(properties.getStockName().split(","));
		} else {
			try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
				stockList = lines.collect(Collectors.toList());
				properties.setStockName(String.join(",", stockList));
			} catch (IOException e) {
				log.error("Error reading file: " + e.getMessage());
				return "No Records";
			}
		}
		StringBuilder emailContent = new StringBuilder();
		List<String> datesOfTheMonth = DateUtil.getDatesOfTheMonth(7);
		for (String date : datesOfTheMonth) {
			if(date.contains("01")){
				continue;
			}
			properties.setStockDate(date);
			properties.setExpiryDate(FormatUtil.getMonthExpiry(date));
			log.info("Processing date: {}", date);
			Map<String, Map<String, FutureAnalysis>> stringMapMap = futureAnalysisService.futureAnalysis(properties);
			for (Map.Entry<String, Map<String, FutureAnalysis>> stringMapEntry : stringMapMap.entrySet()) {
				String stock = stringMapEntry.getKey();
				StringBuilder message = new StringBuilder();
				trendLineService.findTrend(stringMapMap.get(stock), stock, properties, message);
				if (message.length() > 10 && message.toString().contains("short covered")) {
					emailContent.append(message).append("\n");
				}
			}
		}
		if (emailContent.length() < 10) {
			emailContent.append("No Divergence found based on Open Interest");
		}
		return emailContent.toString();
	}

	public String createOptionChainImages(Properties properties) {

		// Path to the file containing the stock list
		String filePath = "src/main/resources/";

		if (properties.getFileName() != null && !properties.getFileName().isEmpty()) {
			filePath = filePath + properties.getFileName() + ".txt";
		} else {
			filePath = filePath + "sectorList.txt";
		}

		// Read all lines from the file into a List
		List<String> stockList;
		if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
			stockList = Arrays.asList(properties.getStockName().split(","));
		} else {
			try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
				stockList = lines.collect(Collectors.toList());
				properties.setStockName(String.join(",", stockList));
			} catch (IOException e) {
				log.error("Error reading file: " + e.getMessage());
				return "No Records";
			}
		}

		MarketMoversResponse marketMoversResponse = ioPulseService.marketMovers(properties);

		if (marketMoversResponse == null || marketMoversResponse.getData() == null || marketMoversResponse.getData().isEmpty()) {
			log.error("No Market Movers data found");
			return "No Market Movers data found";
		}

		for (MarketMoverData marketMoverData : marketMoversResponse.getData()) {
			String stock = marketMoverData.getStSymbolName();
			if (marketMoverData.getInOldOi() == null || marketMoverData.getInNewOi() == null ||
					marketMoverData.getInOldClose() == null || marketMoverData.getInNewClose() == null) {
				log.error("OI or Close data is missing for stock: {}", stock);
				continue;
			}
			double oldOi = Double.parseDouble(marketMoverData.getInOldOi());
			double newOi = Double.parseDouble(marketMoverData.getInNewOi());
			double oldClose = Double.parseDouble(marketMoverData.getInOldClose());
			double newClose = Double.parseDouble(marketMoverData.getInNewClose());
			double ltpChange = newClose - oldClose;
			double percentageChangeInLTP = (ltpChange / oldClose) * 100;
			double percentageChange = ((newOi - oldOi) / oldOi) * 100;
			String oiInterpretation = (percentageChange > 0)
					? (ltpChange > 0 ? "LBU" : "SBU")
					: (ltpChange > 0 ? "SC" : "LU");
			System.out.println("Stock: " + stock + ", OI Change: " + percentageChange +
					", LTP Change: " + percentageChangeInLTP + ", Interpretation: " + oiInterpretation);
			if ((percentageChange > 1 || percentageChange < -1) && (percentageChangeInLTP > 1 || percentageChangeInLTP < -1)) {
				System.out.println("Stock " + stock + " with OI Change " + percentageChange);
				properties.setStartTime("09:15:00");
				properties.setEndTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
				//properties.setEndTime(FormatUtil.getTime(properties.getStartTime(), properties.getInterval()).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
				properties.setExpiryDate("250828");
				System.out.println("Stock " + stock);
				//while (FormatUtil.getTimeHHmmss(properties.getEndTime()).isBefore(FormatUtil.getTimeHHmmss("14:00:00"))) {

					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}

					if (calculateOptionChain.changeInOI(stock, properties.getStartTime(), properties, false, oiInterpretation)) {
						//break;
					}
					//properties.setEndTime(FormatUtil.getTime(properties.getEndTime(), properties.getInterval()).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
				//}
			}
		}
		return "Option Chain Images created successfully for stocks: " + properties.getStockName();
	}


	public List<String> analyseEodResponse(Properties properties) {
		// Path to the file containing the stock list
		String filePath = "src/main/resources/stocksList.txt";

		// Read all lines from the file into a List
		List<String> stockList;
		if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
			stockList = Arrays.asList(properties.getStockName().split(","));
		} else {
			try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
				stockList = lines.collect(Collectors.toList());
			} catch (IOException e) {
				log.error("Error reading file: " + e.getMessage());
				return Collections.EMPTY_LIST;
			}
		}

		List<String> positiveStocks = new ArrayList<>();

		for (String stock : stockList) {

			StockEODResponse eod = ioPulseService.getMonthlyData(stock);
			if (eod == null || eod.getData() == null || eod.getData().size() < 3) {
				log.info("EOD data is insufficient for stock: {}", stock);
				continue;
			}

			if (eod.getData().size() < 15) {
				log.info("EOD data is less than 60 for stock: {}", stock);
				continue;
			}
			for (int i = 0; i < 15; i++) {
				calculateOiInterpretation(eod.getData().get(i), eod.getData().get(i + 1));
			}

			for (int i = 15; i >= 0; i--) {

				FutureEodAnalyzer stockDetails = eod.getData().get(i);
				if ("LBU".equals(stockDetails.getOiInterpretation())
						&& stockDetails.getPercentageChangeInOi() > 2 && stockDetails.getStrength() > 0.8) {
					System.out.println("Positive Stock " + stock + " with LTP " + stockDetails.getInClose() +
							" and OI Change " + stockDetails.getInOi() + " at " + stockDetails.getStFetchDate() +
							" with Strength " + stockDetails.getStrength());
					for (int j = i - 1; j >= 0; j--) {
						FutureEodAnalyzer nextStockDetails = eod.getData().get(j);
						if (nextStockDetails.getInDayLow() > stockDetails.getInDayLow() && nextStockDetails.getInDayLow() < stockDetails.getInDayOpen()) {
							String message = "Positive Stock " + stock + " on  " + nextStockDetails.getStFetchDate() +
									" with respect to " + stockDetails.getStFetchDate() +
									" with Strength " + stockDetails.getStrength();
							positiveStocks.add(message);
						}
					}
				}
			}
		}


		return positiveStocks;
	}


	public List<String> getStocksBasedOnHighChangeInOpenInterest(Properties properties) {

		// Path to the file containing the stock list
		String filePath = "src/main/resources/stocksList.txt";

		// Read all lines from the file into a List
		List<String> stockList = new ArrayList<>();
		if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
			stockList = Arrays.asList(properties.getStockName().split(","));
		} else {
			try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
				stockList = lines.collect(Collectors.toList());
				properties.setStockName(String.join(",", stockList));
			} catch (IOException e) {
				log.error("Error reading file: " + e.getMessage());
			}
		}

		// Read all lines from the file into a List

		List<String> datesOfTheMonth = DateUtil.getWorkingDaysOfMonth(2025, 7);
		System.out.println(datesOfTheMonth);
		List<String> strings = new ArrayList<>();
		for(String stock : stockList){
			properties.setStockName(stock);
			System.out.println("Stock " +stock);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for(String date : datesOfTheMonth) {
			properties.setStockDate(date);
			Map<String, Map<String, FutureAnalysis>> stringMapMap = futureAnalysisService.futureAnalysis(properties);
			for (Map.Entry<String, Map<String, FutureAnalysis>> stringMapEntry : stringMapMap.entrySet()) {
				Map<String, FutureAnalysis> futureAnalysisMap = stringMapEntry.getValue();
				for (Map.Entry<String, FutureAnalysis> futureAnalysisEntry : futureAnalysisMap.entrySet()) {
					String key = futureAnalysisEntry.getKey();
					FutureAnalysis value = futureAnalysisEntry.getValue();
					if (value.getOiPercentageChange() > 0.75) {
						strings.add("Stock " + stock + " , date " + date + " at " + key + " with OI Percentage Change " + value.getOiPercentageChange() +
								" and Interpretation " + value.getInterpretation() + " with Ltp change " + value.getLtpPercentageChange());
						if(futureAnalysisManager.getRecordBySymbolDateAndTime(stock, date, key) == null){
							futureAnalysisManager.saveFutureAnalysis(value);
						}
					}
				}
			}
		}



		}
		return strings;

	}


}
