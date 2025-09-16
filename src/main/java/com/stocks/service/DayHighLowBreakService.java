package com.stocks.service;

import com.stocks.dto.*;
import com.stocks.dto.Properties;
import com.stocks.enumaration.QueryInterval;
import com.stocks.repository.TradeSetupManager;
import com.stocks.utils.DateUtil;
import com.stocks.utils.FormatUtil;
import com.stocks.yahoo.HistQuotesQuery2V8RequestImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DayHighLowBreakService {

    @Autowired
    MailService mailService;

    @Autowired
    private TradeSetupManager tradeSetupManager;

    @Autowired
    CalculateOptionChain calculateOptionChain;

    private static final Map<String, DayLowAndHigh> stocksHistoricalData = new LinkedHashMap<>();

    private static final Map<String, StrikeTO> strikeTOMap = new LinkedHashMap<>();

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

        List<TradeSetupTO> tradeSetups = tradeSetupManager.findTradeSetupsByDateRangeAndStrategy(startDate, endDate, "c2");
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
                double tradeEntry = trade.getEntry2() * 100.2;

                if(openPrice == 0 || closePrice == 0 || lowPrice == 0 || tradeEntry == 0){
                    lastCandle = stockData.get(stockData.size() - 2);
                    openPrice = lastCandle.getOpen().doubleValue();
                    closePrice = lastCandle.getClose().doubleValue();
                    lowPrice = lastCandle.getLow().doubleValue();
                   tradeEntry = trade.getEntry2() * 100.2;
                }

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

    public void checkOptionChainForTradeSetUpStocks(Properties properties) {
        List<TradeSetupTO> tradeSetups = tradeSetupManager.findTradeSetupsByDateAndStrategy(properties.getStockDate(), properties.getStrategy());
        String startTime = "09:15:00";
        String endTime = "15:30:00";
        properties.setStartTime(startTime);
        properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));
        properties.setEndTime(endTime);

        StringBuilder tableContent = new StringBuilder();
        tableContent.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        tableContent.append("<tr>")
                .append("<th>Stock Symbol</th>")
                .append("<th>Time</th>")
                .append("<th>CE OI Chg</th>")
                .append("<th>PE OI Chg</th>")
                .append("<th>Price</th>")
                .append("<th>Time</th>")
                .append("<th>CE OI Chg</th>")
                .append("<th>PE OI Chg</th>")
                .append("<th>Price</th>")
                .append("<th>Time</th>")
                .append("<th>Current CE OI Chg</th>")
                .append("<th>Current PE OI Chg</th>")
                .append("<th>Price</th>")
                .append("<th>Is SC with Price decreasing</th>")
                .append("</tr>");

        for (TradeSetupTO trade : tradeSetups) {
            try {
                StrikeTO currentStrike = calculateOptionChain.getStrike(properties, trade.getStockSymbol(), trade.getEntry2());
                Map<Integer, StrikeTO> strikes = trade.getStrikes();
                StrikeTO strike = strikes.getOrDefault(trade.getEntry2().intValue(), null);

                StrikeTO orDefault = strikeTOMap.getOrDefault(trade.getStockSymbol(), null);

                if (currentStrike != null && strike != null && currentStrike.getCeOiChg() < strike.getCeOiChg() &&
                        currentStrike.getPeOiChg() > strike.getPeOiChg()) {
                    currentStrike.setTime(LocalTime.now().toString());
                    tableContent.append("<tr>")
                            .append("<td>").append(trade.getStockSymbol()).append("</td>")
                            .append("<td>").append(trade.getFetchTime()).append("</td>")
                            .append("<td>").append(strike.getCeOiChg()).append("</td>")
                            .append("<td>").append(strike.getPeOiChg()).append("</td>")
                            .append("<td>").append(strike.getCurPrice()).append("</td>");
                    if (orDefault != null) {
                        tableContent.append("<td>").append(orDefault.getTime()).append("</td>")
                                .append("<td>").append(orDefault.getCeOiChg()).append("</td>")
                                .append("<td>").append(orDefault.getPeOiChg()).append("</td>")
                                .append("<td>").append(orDefault.getCurPrice()).append("</td>");
                    }

                    tableContent.append("<td>").append(currentStrike.getTime()).append("</td>")
                            .append("<td>").append(currentStrike.getCeOiChg()).append("</td>")
                            .append("<td>").append(currentStrike.getPeOiChg()).append("</td>")
                            .append("<td>").append(currentStrike.getCurPrice()).append("</td>");

                    if(orDefault != null) {
                        boolean isScWithPriceDecreasing = currentStrike.getCeOiChg() < orDefault.getCeOiChg() &&
                                currentStrike.getPeOiChg() > orDefault.getPeOiChg() &&
                                currentStrike.getCurPrice() < orDefault.getCurPrice();
                        tableContent.append("<td>").append(isScWithPriceDecreasing ? "Yes" : "No").append("</td>");
                    } else {
                        tableContent.append("<td>N/A</td>");
                    }

                }
                if(currentStrike != null) {
                    strikeTOMap.put(trade.getStockSymbol(), currentStrike);
                }

            } catch (Exception e) {
                System.out.println("Error while checking option chain for " + trade.getStockSymbol());
            }
        }

        tableContent.append("</table>");

        if (!tableContent.toString().contains("<td>")) { // No rows added
            return;
        }

        mailService.sendMail(
                "Option Chain Verification for Trade Setup Stocks on " + properties.getStockDate(),
                "<html><body>" +
                        "<h3>Option Chain Verification Results</h3>" +
                        tableContent.toString() +
                        "</body></html>"
        );
    }

}


