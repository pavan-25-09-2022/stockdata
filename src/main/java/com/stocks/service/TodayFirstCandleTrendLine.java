package com.stocks.service;

import com.stocks.dto.Candle;
import com.stocks.dto.Properties;
import com.stocks.dto.StockData;
import com.stocks.dto.StockResponse;
import com.stocks.utils.FormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class TodayFirstCandleTrendLine {
	@Autowired
	private StockDataManager stockDataManager;
	@Autowired
	private
	ProcessCandleSticks processCandleSticks;
	@Autowired
	private CommonValidation commonValidation;

	public List<StockResponse> getTrendLines(Properties prop) {
		List<StockResponse> stockResponses = new ArrayList<>();
		List<StockData> stocks = new ArrayList<>();
		stocks = stockDataManager.getStocksByDate(FormatUtil.getCurDate(prop));
		List<String> stockNames = prop.getStockName().isEmpty() ? new ArrayList<>() : Arrays.asList(prop.getStockName().split(","));
		for (StockData stock : stocks) {
			if (stockNames.isEmpty() || stockNames.contains(stock.getStock())) {
				StockResponse res = getStock(stock.getStock(), stock.getType(), prop);
				if (res != null) {
					stockResponses.add(res);
				}
			}
		}
		return stockResponses;
	}

	private StockResponse getStock(String stock, String type, Properties prop) {
		List<Candle> candles = commonValidation.getCandles(prop, stock);
		candles = candles.subList(1, candles.size()); // ignore first candle
		if (candles.size() > 2) {
			List<Candle> newCandles = new ArrayList<>();
			int totalCandleSticks = candles.size();
			newCandles.add(candles.get(0));
			newCandles.add(candles.get(totalCandleSticks - 1));
			for (totalCandleSticks = candles.size(); totalCandleSticks > 2; totalCandleSticks--) {
				StockResponse res = processCandleSticks.getStockResponse(stock, prop, newCandles, new ArrayList<>());
				if (res != null) {
					if (type.equals("P")) {
						res.setStockType("P");
						Candle firstCandle = res.getFirstCandle();
						Candle curCandle = res.getCurCandle();
						if (("SC".equals(res.getOiInterpretation()) || "LBU".equals(res.getOiInterpretation())) && firstCandle != null && curCandle != null) {
							double firstCandleLow = firstCandle.getLow();
							double curCandleLow = curCandle.getLow();
							if (curCandleLow < firstCandleLow) {
								return res;
							}
						}
					} else if (type.equals("N")) {
						res.setStockType("N");
						Candle firstCandle = res.getFirstCandle();
						Candle curCandle = res.getCurCandle();
						if (("SBU".equals(res.getOiInterpretation()) || "LU".equals(res.getOiInterpretation())) && firstCandle != null && curCandle != null) {
							double firstCandleHigh = firstCandle.getHigh();
							double curCandleHigh = curCandle.getHigh();
							if (curCandleHigh > firstCandleHigh) {
								return res;
							}
						}
					}
				}
				newCandles.add(candles.get(totalCandleSticks - 1));
			}

		}
		return null;
	}

}
