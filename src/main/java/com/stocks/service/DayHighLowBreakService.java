package com.stocks.service;

import com.stocks.dto.DayLowAndHigh;
import com.stocks.dto.HistoricalQuote;
import com.stocks.dto.Properties;
import com.stocks.dto.TradeSetupTO;
import com.stocks.enumaration.QueryInterval;
import com.stocks.repository.TradeSetupManager;
import com.stocks.utils.DateUtil;
import com.stocks.yahoo.HistQuotesQuery2V8RequestImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
class DayHighLowBreakService {

    @Autowired
    MailService mailService;

    @Autowired
    private TradeSetupManager tradeSetupManager;

    private static final Map<String, DayLowAndHigh> stocksHistoricalData = new LinkedHashMap<>();

   public void testDayHighLow()  {

        Calendar from = DateUtil.start();
        Calendar to = DateUtil.end();

        List<String> stocks = new ArrayList<>();
        stocks.add("HDFCBANK.NS");
        stocks.add("RELIANCE.NS");
        stocks.add("ICICIBANK.NS");
        stocks.add("INFY.NS");

        if(stocksHistoricalData.isEmpty()) {
            for (String stock : stocks) {
                DayLowAndHigh dayLowAndHigh = new DayLowAndHigh(BigDecimal.ZERO, BigDecimal.ZERO, stock, false, false);
                stocksHistoricalData.put(stock, dayLowAndHigh);
            }
        }

        try {
            getStockDetails(stocks, from, to, stocksHistoricalData, 5);
        } catch (Exception e) {

        }


    }

    public void alertStocksBasedOnLastCandle(String startDate, String endDate) throws IOException {
       if(!StringUtils.hasText(startDate) || !StringUtils.hasText(endDate)){
           return;
       }

        List<TradeSetupTO> tradeSetups = tradeSetupManager.findTradeSetupsBetweenDates(startDate, endDate);
        Calendar from = DateUtil.start();
        Calendar to = DateUtil.end();

        List<String> alertStocks = new ArrayList<>();

        for (TradeSetupTO trade : tradeSetups) {
            HistQuotesQuery2V8RequestImpl impl = new HistQuotesQuery2V8RequestImpl(trade.getStockSymbol() + ".NS", from, to, QueryInterval.FIVE_MINS);
            List<HistoricalQuote> stockData = impl.getCompleteResult();

            if (stockData != null && !stockData.isEmpty()) {
                HistoricalQuote lastCandle = stockData.get(stockData.size() - 1);
                double openPrice = lastCandle.getOpen().doubleValue();
                double closePrice = lastCandle.getClose().doubleValue();
                double lowPrice = lastCandle.getLow().doubleValue();
                double tradeEntry = trade.getEntry2().doubleValue() * 100.2;

                if (openPrice > tradeEntry && (closePrice < tradeEntry || lowPrice < tradeEntry) ) {
                    alertStocks.add(trade.getStockSymbol() + " meets the criteria: Open=" + openPrice + ", Close=" + closePrice + ", Low=" + lowPrice + ", Trade Entry=" + tradeEntry);
                }
            }
        }

        if (!alertStocks.isEmpty()) {
            mailService.sendMail("Stocks Alert Based on Last Candle", String.join("\n", alertStocks));
        }
    }


    public void getStockDetails(List<String> stocks, Calendar from, Calendar to, Map<String, DayLowAndHigh> stocksHistoricalData,
                                int timeInterval) throws IOException {

        QueryInterval queryInterval = timeInterval == 5 ? QueryInterval.FIVE_MINS : QueryInterval.FIFTEEN_MINS;

        for (String stock : stocks) {

            HistQuotesQuery2V8RequestImpl impl = new HistQuotesQuery2V8RequestImpl(stock, from, to, QueryInterval.DAILY);

            List<HistoricalQuote> stockData = impl.getCompleteResult();

            DayLowAndHigh dayLowAndHigh = stocksHistoricalData.get(stock);

            if (dayLowAndHigh.getDayHigh().equals(BigDecimal.ZERO) || dayLowAndHigh.getDayLow().equals(BigDecimal.ZERO)) {

                for (HistoricalQuote quote : stockData) {
                    dayLowAndHigh.setDayHigh(quote.getDayHigh().add(new BigDecimal(0.1)));
                    dayLowAndHigh.setDayLow(quote.getDayLow().subtract(new BigDecimal(0.1)));
                }
            } else {

                for (HistoricalQuote quote : stockData) {

                    if (quote.getDayHigh().compareTo(dayLowAndHigh.getDayHigh()) > 0) {
                        dayLowAndHigh.setDayHigh(quote.getDayHigh().add(new BigDecimal(0.1)));
                        dayLowAndHigh.setDayHighBreak(true);
                    } else {
                        dayLowAndHigh.setDayHighBreak(false);
                    }

                    if (quote.getDayLow().compareTo(dayLowAndHigh.getDayLow()) < 0) {
                        dayLowAndHigh.setDayLow(quote.getDayLow().subtract(new BigDecimal(0.1)));
                        dayLowAndHigh.setDayLowBreak(true);
                    } else {
                        dayLowAndHigh.setDayLowBreak(false);
                    }
                }

            }


        }

        boolean isDayLow = false;
        boolean isdayHigh = false;
        List<String> breakHighStocks = new ArrayList<>();
        List<String> breakLowStocks = new ArrayList<>();
        for (String stock : stocks) {
            DayLowAndHigh dayLowAndHigh = stocksHistoricalData.get(stock);
            if (dayLowAndHigh.isDayHighBreak()) {
                breakHighStocks.add(dayLowAndHigh.getStock() + " breaks day High");
                isdayHigh = true;
            }
            if (dayLowAndHigh.isDayLowBreak()) {
                breakLowStocks.add(dayLowAndHigh.getStock() + " breaks day's low");
                isDayLow = true;
            }
        }
        if (isdayHigh) {
            mailService.sendMail("Day High Breaks", String.join("\n", breakHighStocks));
        }

        if (isDayLow) {
            mailService.sendMail("Day Low Breaks", String.join("\n", breakLowStocks));
        }

    }


}


