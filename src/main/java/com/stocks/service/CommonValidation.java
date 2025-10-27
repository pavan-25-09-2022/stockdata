package com.stocks.service;

import com.stocks.dto.ApiResponse;
import com.stocks.dto.Candle;
import com.stocks.dto.CandleDataResponse;
import com.stocks.dto.Properties;
import com.stocks.dto.StockProfitResult;
import com.stocks.dto.StockResponse;
import com.stocks.dto.TrendingOiData;
import com.stocks.utils.DateUtil;
import com.stocks.utils.FormatUtil;
import com.stocks.utils.MarketHolidayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CommonValidation {
	@Autowired
	private IOPulseService ioPulseService;
	@Autowired
	private CandlePattern candlePattern;

	private static final int MINS = 5;
	private static final LocalTime START_TIME = LocalTime.of(9, 16, 0); // Configurable start time
	private static final Logger log = LoggerFactory.getLogger(CommonValidation.class);

	public Candle validateYstStocks(String stock, Properties properties, List<Candle> candles, Candle lastCandle) {
		Properties prop = new Properties();
		prop.setStockDate(FormatUtil.addDays(properties.getStockDate(), -2));
		prop.setStockName(properties.getStockName());
		List<Candle> ystCandles = getCandles(prop, stock);
		// ignore yest EOD candle
		ystCandles = ystCandles.subList(1, candles.size());
//        Properties prop1 = new Properties();
//        prop1.setStockDate(FormatUtil.getYesterdayDate(properties.getStockDate(), 3));
//        prop1.setStockName(properties.getStockName());
//        List<Candle> ystCandles1 = getCandles(prop1,stock );
//        Properties prop2 = new Properties();
//        prop2.setStockDate(FormatUtil.getYesterdayDate(properties.getStockDate(), 4));
//        prop2.setStockName(properties.getStockName());
//        List<Candle> ystCandles2 = getCandles(prop2,stock );
//        Properties prop3 = new Properties();
//        prop3.setStockDate(FormatUtil.getYesterdayDate(properties.getStockDate(), 5));
//        prop3.setStockName(properties.getStockName());
//        List<Candle> ystCandles3 = getCandles(prop3,stock );
		List<Candle> candleList = new ArrayList<>();
//        candleList.addAll(ystCandles3);
//        candleList.addAll(ystCandles2);
//        candleList.addAll(ystCandles1);
		candleList.addAll(ystCandles);
		candleList.addAll(candles);
		for (Candle c : candleList) {
//                    if(c.getVolume() > todayAvgVol)
			{
				// Check that lastCandle data is valid
				// Top wick should not be greater than the candle body size

				if (c.getVolume() > lastCandle.getVolume() && ("LU".equals(c.getOiInt()) || "SBU".equals(c.getOiInt()))) {
					if (lastCandle.getClose() >= Math.min(c.getLow() * 1.005, c.getHigh())
							&& lastCandle.getClose() <= Math.max(c.getOpen(), c.getHigh() * 1.005)) {
//                                log.info("price now, candle high, candle open: {}, {}, {}", priceNow, c.getHigh(), c.getOpen());
//                            if((c.getStrength() > lastCandle.getStrength()))
						{
//                                    log.info("OI % {} Strength: {} - {} {} at {} - cur OI % {} cur Strength {} - {} {} at {}", c.getCurOiPer(), c.getStrength(), c.getOiChange(), c.getLtpChange(), c.getEndTime(), lastCandle.getCurOiPer(), lastCandle.getStrength(), lastCandle.getOiChange(), lastCandle.getLtpChange(), lastCandle.getEndTime());
							return c;
						}
					}
				}
			}
		}
		return null;
	}

	public void checkExitSignal(StockResponse res, Candle cur) {
		double entryPrice = res.getCurCandle().getClose();
		double stopLoss = entryPrice * 0.99;
		double targetPrice = entryPrice * 1.0085;
//        List<Double> closePrices = candles.stream()
//                .map(Candle::getClose)
//                .collect(Collectors.toList());
//        double vwap = calculateVWAP(candles);
//        double rsi = calculateRSI(closePrices, 14);
//        Candle cur = candles.get(candles.size() - 1);

		double currentPrice = cur.getClose();
		if (currentPrice <= stopLoss) {
//            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", 0.0), String.format("%.2f", 0.0),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
			log.info("🚨 Exit — {} Stop-loss hit at {} is positive {} ", res.getStock(), cur.getEndTime(), entryPrice < currentPrice);
//            StockProfitResult profitResult = new StockProfitResult();
//            profitResult.setSellTime(cur.getEndTime().toString() + " S");
//            res.setStockProfitResult(profitResult);

//            res.setStock(null);
		} else if (currentPrice >= targetPrice) {
//            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", 0.0), String.format("%.2f", 0.0),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
//            log.info("🎯 Exit — Target reached at {} is positive {} ", cur.getEndTime(),entryPrice<currentPrice);
			StockProfitResult profitResult = new StockProfitResult();
			profitResult.setSellPrice(currentPrice);
			profitResult.setProfit(entryPrice);
			profitResult.setSellTime(cur.getEndTime().toString() + " T");
			res.setStockProfitResult(profitResult);
		}
//        else if (rsi > 80) {
//            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", vwap), String.format("%.2f", rsi),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
//            log.info("📉 Exit — RSI Overbought + Below VWAP (Weakness) at {} is positive {} ", cur.getEndTime(),res.getCurrentPrice()<currentPrice);
//            res.setStock(null);
//        }
//        else if (rsi < 30) {
//            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", vwap), String.format("%.2f", rsi),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
//            log.info("⚠️ Exit — RSI Oversold bounce failing at {} is positive {} ", cur.getEndTime(),res.getCurrentPrice()<currentPrice);
////            res.setStock(null);
//        }
		if ((cur.getEndTime().equals(LocalTime.of(15, 0)))) {
//            log.info("Current: {} | VWAP: {} | RSI: {} | entryPrice {} | stoploss: {} | target: {}", String.format("%.2f", currentPrice), String.format("%.2f", 0.0), String.format("%.2f", 0.0),String.format("%.2f", entryPrice),String.format("%.2f", stopLoss), String.format("%.2f", targetPrice));
//            log.info("End of day reached for {} , exiting strategy. is positive {} time {} ", res.getStock(), entryPrice<currentPrice, cur.getEndTime());
//            res.setStock(null);
			StockProfitResult profitResult = new StockProfitResult();

			profitResult.setSellPrice(currentPrice);
			profitResult.setSellTime(cur.getEndTime().toString() + (entryPrice < currentPrice ? " P" : " L"));
//            res.setStockProfitResult(profitResult);
		}
	}

	public double calculateVWAP(List<Candle> candles) {
		double pvSum = 0.0, volSum = 0.0;
		for (Candle c : candles) {
			double tp = c.typicalPrice();
			pvSum += tp * c.getVolume();
			volSum += c.getVolume();
		}
		return (volSum == 0) ? 0 : pvSum / volSum;
	}

	public double calculateRSI(List<Double> closes, int period) {
		if (closes.size() <= period) return 0;

		double gain = 0, loss = 0;
		for (int i = 1; i <= period; i++) {
			double change = closes.get(i) - closes.get(i - 1);
			if (change > 0) gain += change;
			else loss += -change;
		}

		double avgGain = gain / period;
		double avgLoss = loss / period;

		for (int i = period + 1; i < closes.size(); i++) {
			double change = closes.get(i) - closes.get(i - 1);
			if (change > 0) {
				avgGain = (avgGain * (period - 1) + change) / period;
				avgLoss = (avgLoss * (period - 1)) / period;
			} else {
				avgGain = (avgGain * (period - 1)) / period;
				avgLoss = (avgLoss * (period - 1) + -change) / period;
			}
		}

		if (avgLoss == 0) return 100;

		double rs = avgGain / avgLoss;
		return 100 - (100 / (1 + rs));
	}

	public Candle getCandleByTime(List<Candle> list, String time) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		for (Candle c : list) {
			if (c.getEndTime().toString().equals(time)) {
				return c;
			}
		}
		return null;
	}

	public boolean isPositive(List<Candle> candles, Candle firstCandle, Candle curCandle, String oiInterpretation) {
		
		return ((oiInterpretation.equals("LBU") || oiInterpretation.equals("SC")));
//        return curCandle.getClose() > firstCandle.getHigh() && highToOpenChge < 0.4 && curCandle.getLow() < firstCandle.getHigh() && (oiInterpretation.equals("LBU"))
	}

	private boolean IsLBUorSC(List<Candle> candles) {
		Candle cur = candles.get(candles.size() - 1);
		Candle prev = candles.get(candles.size() - 2);
		return "LBU".equals(cur.getOiInt())
//                || ("SC".equals(prev.getOiInt()) || "LBU".equals(prev.getOiInt()))
				;
	}


	@Deprecated
	/**
	 * This method is deprecated and should not be used in new code.
	 * use getCandles(Properties properties, String from, String to) instead.
	 */
	public List<Candle> getCandles(Properties properties, String stock) {
		List<Candle> candles = new ArrayList<>();
		// Make POST request
		ApiResponse apiResponse = ioPulseService.sendRequest(properties, stock);

		// Process response
//        ApiResponse apiResponse = response.getBody();
		if (apiResponse == null || apiResponse.getData() == null || apiResponse.getData().isEmpty() || apiResponse.getData().size() < 5) {
//            log.info("Data is insufficient for " + stock);
			return candles;
		}
		List<ApiResponse.Data> list = apiResponse.getData();
//        List<Double> rsiList = new ArrayList<>();
//        ApiResponse.Data previousData;
//        long fistCandleVol = 0;
//        List<Long> volumes = new ArrayList<>();
		long previousVolume = 0;
		double firstCandleHigh = 0.0;
		double firstCandleLow = 0.0;
		List<List<ApiResponse.Data>> chunks = chunkByMinutes(list, properties.getInterval());
		if (chunks == null || chunks.isEmpty()) {
			log.info("Data size is less than 3 for stock: " + stock);
			return candles;
		}
		ApiResponse.Data previousEodChunk = chunks.get(0).get(0);

		if (previousEodChunk != null) {
			Candle previousEodCandle = new Candle();
			previousEodCandle.setOpen(previousEodChunk.getOpen());
			previousEodCandle.setHigh(previousEodChunk.getHigh());
			previousEodCandle.setLow(previousEodChunk.getLow());
			previousEodCandle.setClose(previousEodChunk.getClose());
			candles.add(previousEodCandle);
		}

		List<ApiResponse.Data> firstCandleChunk = chunks.get(1);
//        if(firstCandleChunk.size() < 3){
//            return null;
//        }
		double firstCandleClose = 0.0;
		double firstCandleOpen = 0.0;
		Candle firstCandleStick = new Candle();
		for (ApiResponse.Data data : firstCandleChunk) {
			previousVolume += data.getTradedVolume();
			if (firstCandleHigh == 0.0) {
				firstCandleHigh = data.getHigh();
			}
			if (firstCandleLow == 0.0) {
				firstCandleLow = data.getLow();
			}
			firstCandleClose = data.getClose();
			if (firstCandleOpen == 0.0) {
				firstCandleOpen = data.getOpen();
			}
			firstCandleHigh = Math.max(data.getHigh(), firstCandleHigh);
			firstCandleLow = Math.min(data.getLow(), firstCandleLow);
			firstCandleStick.setOpenInterest(Long.parseLong(data.getOpenInterest()));

		}
		firstCandleStick.setHigh(firstCandleHigh);
		firstCandleStick.setLow(firstCandleLow);
		firstCandleStick.setClose(firstCandleClose);
		firstCandleStick.setOpen(firstCandleOpen);
		firstCandleStick.setVolume(previousVolume);
		firstCandleStick.setCount(firstCandleChunk.size());
		double oiPert = ((double) firstCandleStick.getOiChange() / (firstCandleStick.getOpenInterest() - firstCandleStick.getOiChange())) * 100;
		firstCandleStick.setCurOiPer(Double.valueOf(String.format("%.2f", oiPert)));
		firstCandleStick.setStartTime(FormatUtil.getTimeHHmm("09:15"));
		LocalTime endTime = FormatUtil.getTime("09:15:00", Math.min(properties.getInterval(), 15));
		firstCandleStick.setEndTime(endTime);
		double firstCandleLtpChange = firstCandleStick.getClose() - previousEodChunk.getClose();
		firstCandleLtpChange = Double.valueOf(String.format("%.2f", firstCandleLtpChange));
		long firstCandleOiChange = firstCandleStick.getOpenInterest() - Long.parseLong(previousEodChunk.getOpenInterest());
		String firstCandleOiInterpretation = (firstCandleOiChange > 0)
				? (firstCandleLtpChange > 0 ? "LBU" : "SBU")
				: (firstCandleLtpChange > 0 ? "SC" : "LU");
		firstCandleStick.setOiChange(firstCandleOiChange);
		firstCandleStick.setOiInt(firstCandleOiInterpretation);
		candles.add(firstCandleStick);
		long highVolume = previousVolume;
		Candle prevCandle = firstCandleStick;
		for (int i = 2; i < chunks.size(); i++) {
			List<ApiResponse.Data> chunk = chunks.get(i);
			long totalVolume = 0;
			ApiResponse.Data firstCandle = null;

			double curHigh = 0.0;
			double curLow = 0.0;
			double curOpen = 0.0;
			double curClose = 0.0;
			long specificHighVolume = 0;
			Candle candle = new Candle();
			for (ApiResponse.Data data : chunk) {
				totalVolume += data.getTradedVolume();
				if (specificHighVolume == 0.0) {
					specificHighVolume = data.getTradedVolume();
				}
				if (data.getTradedVolume() > specificHighVolume) {
					specificHighVolume = data.getTradedVolume();
				}
				if (firstCandle == null) {
					firstCandle = data;
				}
				if (curOpen == 0.0) {
					curOpen = data.getOpen();
				}
				curClose = data.getClose();
				if (curHigh == 0.0) {
					curHigh = data.getHigh();
				}
				if (curLow == 0.0) {
					curLow = data.getLow();
				}
				curHigh = Math.max(data.getHigh(), curHigh);
				curLow = Math.min(data.getLow(), curLow);
				candle.setOpenInterest(Long.parseLong(data.getOpenInterest()));
			}
			candle.setVolume(totalVolume);
			boolean isHigher = false;
			if (highVolume < candle.getVolume()) {
				isHigher = true;
				highVolume = candle.getVolume();
			}
			candle.setHigh(curHigh);
			candle.setLow(curLow);
			candle.setClose(curClose);
			candle.setOpen(curOpen);
			candle.setVolume(totalVolume);
			candle.setCount(chunk.size());
			candle.setStartTime(endTime);
			candle.setHighVolume(isHigher);
			endTime = endTime.plusMinutes(properties.getInterval());
			candle.setEndTime(endTime);

			double ltpChange = candle.getClose() - prevCandle.getClose();
			ltpChange = Double.valueOf(String.format("%.2f", ltpChange));
			long oiChange = candle.getOpenInterest() - prevCandle.getOpenInterest();
			String oiInterpretation = (oiChange > 0)
					? (ltpChange > 0 ? "LBU" : "SBU")
					: (ltpChange > 0 ? "SC" : "LU");
			candle.setOiChange(oiChange);
			candle.setOiInt(oiInterpretation);
			double curPer = ((double) candle.getOiChange() / (candle.getOpenInterest() - candle.getOiChange())) * 100;
			candle.setCurOiPer(Double.valueOf(String.format("%.2f", curPer)));
			candle.setLtpChange(Double.valueOf(String.format("%.2f", (candle.getClose() - prevCandle.getClose()))));
			candles.add(candle);
			candle.setPatternType(candlePattern.evaluate(candles));
//			log.info("Candle: {}-{} : {} - {} - {} - {} - {} - {} - {} - {} - {}", stock, properties.getStockDate(), candle.getStartTime(), candle.getEndTime(), candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose(), candle.getVolume(), candle.getOiInt(), candle.getPatternType());
			prevCandle = candle;
		}
		return candles;
	}

	public List<Candle> getCandles(Properties properties, String from, String to) {
		long fromEpoch = DateUtil.getEpochTimeFromString(from);
		long toEpoch = DateUtil.getEpochTimeFromString(to);

		CandleDataResponse candleDataResponse = ioPulseService.getCandleData(properties, fromEpoch, toEpoch);
		if (candleDataResponse == null) {
			log.error("Candle data response is null for stock: {} from: {} to: {}", properties.getStockName(), from, to);
			return Collections.emptyList();
		}
		List<Candle> candles = new ArrayList<>();
		for (int i = 0; i < candleDataResponse.getData().size(); i++) {
			CandleDataResponse.Data candleData = candleDataResponse.getData().get(i);
//			CandleDataResponse.Data beforeCandle = candleDataResponse.getData().get(i - 1);
			Candle candle = new Candle();
			candle.setOpen(candleData.getOpen());
			candle.setHigh(candleData.getHigh());
			candle.setLow(candleData.getLow());
			candle.setClose(candleData.getClose());
			candle.setVolume(candleData.getVolume());
			if (StringUtils.hasText(candleData.getOi())) {
				candle.setOpenInterest(Long.parseLong(candleData.getOi()));
			}
			long epochSeconds = (long) Double.parseDouble(candleData.getTime());
			LocalTime startTime = Instant.ofEpochSecond(epochSeconds)
					.atZone(ZoneId.systemDefault())
					.toLocalTime();
			LocalDate date = Instant.ofEpochSecond(epochSeconds)
					.atZone(ZoneId.systemDefault())
					.toLocalDate();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
			candle.setStartTime(FormatUtil.getTimeHHmm(startTime.format(formatter)));

			int interval = properties.getInterval(); // interval in minutes
			LocalTime endTime = startTime.plusMinutes(interval);
			candle.setEndTime(FormatUtil.getTimeHHmm(endTime.format(formatter)));
			DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			candle.setDate(date.format(formatter1));
//			double ltpChange = candle.getClose() - beforeCandle.getClose();
//			ltpChange = Double.parseDouble(String.format("%.2f", ltpChange));
//			long oiChange = candle.getOpenInterest() - Long.parseLong(beforeCandle.getOi());
//			String oiInterpretation = (oiChange > 0)
//					? (ltpChange > 0 ? "LBU" : "SBU")
//					: (ltpChange > 0 ? "SC" : "LU");
//			candle.setOiChange(oiChange);
//			candle.setOiInt(oiInterpretation);
//			double curPer = ((double) candle.getOiChange() / (candle.getOpenInterest() - candle.getOiChange())) * 100;
//			candle.setCurOiPer(Double.parseDouble(String.format("%.2f", curPer)));
//			candle.setLtpChange(Double.parseDouble(String.format("%.2f", (candle.getClose() - beforeCandle.getClose()))));
			candles.add(candle);
		}
		return candles;
	}

	public List<List<ApiResponse.Data>> chunkByMinutes(List<ApiResponse.Data> dataList, int minutes) {

		if (dataList == null || dataList.isEmpty()) {
			log.warn("Data list is null or empty, returning an empty list.");
			return Collections.emptyList();
		}

		int finalMinutes = minutes != 0 ? minutes : MINS;

		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		// Group data by intervals
		Map<LocalTime, List<ApiResponse.Data>> groupedData = dataList.stream()
				.collect(Collectors.groupingBy(
						data -> {
							LocalTime time = LocalTime.parse(data.getTime(), timeFormatter);
							if (finalMinutes > 15) {
								LocalTime firstGroupEnd = START_TIME.plusMinutes(14);
								if (!time.isAfter(firstGroupEnd)) {
									return START_TIME.plusSeconds(0);
								} else {
									int minutesSince930 = (int) java.time.Duration.between(LocalTime.of(9, 31), time).toMinutes();
									int minuteBucket = (minutesSince930 / finalMinutes) * finalMinutes;
									return LocalTime.of(9, 31).plusMinutes(minuteBucket).withSecond(0);
								}
							} else {
								int minutesSinceStart = (int) java.time.Duration.between(START_TIME, time).toMinutes();
								int minuteBucket = (minutesSinceStart / finalMinutes) * finalMinutes;
								return START_TIME.plusMinutes(minuteBucket).withSecond(0);
							}
						},
						LinkedHashMap::new, // Maintain order
						Collectors.toList()
				));
		return new ArrayList<>(groupedData.values());
	}


	public List<List<TrendingOiData>> chunkByMinutesForTrendingOI(List<TrendingOiData> dataList, int minutes) {
		if (dataList == null || dataList.isEmpty()) {
			log.warn("Data list is null or empty, returning an empty list.");
			return Collections.emptyList();
		}

		int finalMinutes = minutes != 0 ? minutes : MINS;

		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		// Group data by intervals
		Map<LocalTime, List<TrendingOiData>> groupedData = dataList.stream()
				.collect(Collectors.groupingBy(
						data -> {
							LocalTime time = LocalTime.parse(data.getStTime(), timeFormatter);
							if (finalMinutes > 15) {
								LocalTime firstGroupEnd = START_TIME.plusMinutes(14);
								if (!time.isAfter(firstGroupEnd)) {
									return START_TIME.plusSeconds(0);
								} else {
									int minutesSince930 = (int) java.time.Duration.between(LocalTime.of(9, 31), time).toMinutes();
									int minuteBucket = (minutesSince930 / finalMinutes) * finalMinutes;
									return LocalTime.of(9, 31).plusMinutes(minuteBucket).withSecond(0);
								}
							} else {
								int minutesSinceStart = (int) java.time.Duration.between(START_TIME, time).toMinutes();
								int minuteBucket = (minutesSinceStart / finalMinutes) * finalMinutes;
								return START_TIME.plusMinutes(minuteBucket).withSecond(0);
							}
						},
						LinkedHashMap::new, // Maintain order
						Collectors.toList()
				));

		return new ArrayList<>(groupedData.values());
	}

	/**
	 * Get historical candles for advanced stop loss calculation
	 */
	public List<Candle> getHistoricalCandles(Properties properties, String endTime) {
		try {

			String startDateTime = properties.getStockDate() + " 09:15";
			String endDateTime = properties.getStockDate() + " " + endTime;

			List<Candle> candles = getCandles(properties, startDateTime, endDateTime);
			List<Candle> ystCandles = new ArrayList<>();
			if (candles.size() < 50) {
				String startDateTime1 = MarketHolidayUtils.getWorkingDay(FormatUtil.addDays(properties.getStockDate(), -1)) + " 09:15";
				String endDateTime1 = MarketHolidayUtils.getWorkingDay(FormatUtil.addDays(properties.getStockDate(), -1)) + " 15:15";

				ystCandles = getCandles(properties, startDateTime1, endDateTime1);
			}
			// Reverse the list to have chronological order (oldest first)
			List<Candle> chronologicalCandles = new ArrayList<>();
			chronologicalCandles.addAll(ystCandles);
			chronologicalCandles.addAll(candles);

//			java.util.Collections.reverse(chronologicalCandles);

			return chronologicalCandles;
		} catch (Exception e) {
			log.error("Error getting historical candles for {}: {}", properties.getStockDate(), e.getMessage());
			return new ArrayList<>();
		}
	}
}
