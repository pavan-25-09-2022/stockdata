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

    @Autowired
    IOPulseService ioPulseService;

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

    public void alertStocksBasedOnLastCandle(String startDate, String endDate, String strategy) throws IOException {
        if (!StringUtils.hasText(startDate) || !StringUtils.hasText(endDate)) {
            return;
        }

        List<TradeSetupTO> tradeSetups = tradeSetupManager.findTradeSetupsByDateRangeAndStrategy(startDate, endDate, strategy);
        Calendar from = DateUtil.start();
        Calendar to = DateUtil.end();

        StringBuilder tableContent = new StringBuilder();
        tableContent.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        tableContent.append("<tr>")
                .append("<th>Stock Symbol</th>")
                .append("<th>Trade Entry</th>")
                .append("<th>Date</th>")
                .append("<th>Open Price</th>")
                .append("<th>Close Price</th>")
                .append("<th>Low Price</th>")
                .append("<th>Strategy</th>")
                .append("<th>Current CE OI</th>")
                .append("<th>Current PE OI</th>")
                .append("<th>Change in CE OI</th>")
                .append("<th>Change in PE OI</th>")
                .append("</tr>");

        for (TradeSetupTO trade : tradeSetups) {
            HistQuotesQuery2V8RequestImpl impl = new HistQuotesQuery2V8RequestImpl(trade.getStockSymbol() + ".NS", from, to, QueryInterval.FIVE_MINS);
            List<HistoricalQuote> stockData = impl.getCompleteResult();

            if (stockData != null && !stockData.isEmpty()) {
                HistoricalQuote lastCandle = stockData.get(stockData.size() - 2);
                double openPrice = lastCandle.getOpen().doubleValue();
                double closePrice = lastCandle.getClose().doubleValue();
                double lowPrice = lastCandle.getLow().doubleValue();
                double tradeEntry = trade.getEntry2();
                double tradeEntryThreshold = trade.getEntry2() * 1.002;

                if (openPrice == 0 || closePrice == 0 || lowPrice == 0 || tradeEntry == 0) {
                    lastCandle = stockData.get(stockData.size() - 3);
                    openPrice = lastCandle.getOpen().doubleValue();
                    closePrice = lastCandle.getClose().doubleValue();
                    lowPrice = lastCandle.getLow().doubleValue();
                    tradeEntry = trade.getEntry2();
                    tradeEntryThreshold = trade.getEntry2() * 1.002;
                }

                if ((openPrice > tradeEntry && lowPrice < tradeEntry && closePrice > tradeEntry) ||
                        (openPrice > tradeEntryThreshold && lowPrice < tradeEntryThreshold)) {
                    Properties properties = buildProperties();
                    StrikeTO currentStrike = calculateOptionChain.getStrike(properties, trade.getStockSymbol(), trade.getEntry2());
                    tableContent.append("<tr>")
                            .append("<td>").append(trade.getStockSymbol()).append("</td>")
                            .append("<td>").append(tradeEntry).append("</td>")
                            .append("<td>").append(trade.getStockDate()).append("</td>")
                            .append("<td>").append(openPrice).append("</td>")
                            .append("<td>").append(closePrice).append("</td>")
                            .append("<td>").append(lowPrice).append("</td>")
                            .append("<td>").append(strategy).append("</td>")
                            .append("<td>").append(currentStrike != null ? FormatUtil.formatIndianNumber(currentStrike.getCeOi()) : "N/A").append("</td>")
                            .append("<td>").append(currentStrike != null ? FormatUtil.formatIndianNumber(currentStrike.getPeOi()) : "N/A").append("</td>")
                            .append("<td>").append(currentStrike != null ? FormatUtil.formatIndianNumber((int) currentStrike.getCeOiChg()) : "N/A").append("</td>")
                            .append("<td>").append(currentStrike != null ? FormatUtil.formatIndianNumber((int) currentStrike.getPeOiChg()) : "N/A").append("</td>")
                            .append("</tr>");
                }
            }
        }

        tableContent.append("</table>");

        if (!tableContent.toString().contains("<td>")) { // No rows added
            return;
        }

        mailService.sendMail(
                "Stocks Alert Based on Last Candle",
                "<html><body>" +
                        "<h3>Stocks Alert Based on Last Candle</h3>" +
                        tableContent.toString() +
                        "</body></html>"
        );
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

        MarketMoversResponse marketMoversResponse = ioPulseService.marketMovers(properties);
        if (marketMoversResponse != null && marketMoversResponse.getData() != null && !marketMoversResponse.getData().isEmpty()) {
            for (MarketMoverData marketMoverData : marketMoversResponse.getData()) {
                String stock = marketMoverData.getStSymbolName();
                for (TradeSetupTO tradeSetupTO : tradeSetups) {
                    if (stock.equals(tradeSetupTO.getStockSymbol())) {
                        double oldOi = Double.parseDouble(marketMoverData.getInOldOi());
                        double newOi = Double.parseDouble(marketMoverData.getInNewOi());
                        double oldClose = Double.parseDouble(marketMoverData.getInOldClose());
                        double newClose = Double.parseDouble(marketMoverData.getInNewClose());
                        double ltpChg = newClose - oldClose;
                        double lptChgPer = (ltpChg / oldClose) * 100;
                        double oiChg = ((newOi - oldOi) / oldOi) * 100;
                        tradeSetupTO.setOiChgPer(oiChg);
                        tradeSetupTO.setLtpChgPer(lptChgPer);
                    }
                }
            }
        }

        StringBuilder tableContent = new StringBuilder();
        tableContent.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        tableContent.append("<tr>")
                .append("<th>Stock Symbol</th>")
                .append("<th>OI change</th>")
                .append("<th>Ltp change</th>")
                .append("<th>Strike</th>")
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

        StringBuilder existStocks = new StringBuilder();
        existStocks.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        existStocks.append("<tr>")
                .append("<th>Stock Symbol</th>")
                .append("<th>OI change</th>")
                .append("<th>Ltp change</th>")
                .append("<th>Strike</th>")
                .append("<th>Time</th>")
                .append("<th>CE OI Chg</th>")
                .append("<th>PE OI Chg</th>")
                .append("<th>Price</th>")
                .append("<th>Time</th>")
                .append("<th>CE OI Chg</th>")
                .append("<th>PE OI Chg</th>")
                .append("<th>Price</th>")
                .append("</tr>");

        for (TradeSetupTO trade : tradeSetups) {
            try {
                StrikeTO currentStrike = calculateOptionChain.getStrike(properties, trade.getStockSymbol(), trade.getEntry2());
                Map<Integer, StrikeTO> strikes = trade.getStrikes();
                StrikeTO strike = strikes.getOrDefault(trade.getEntry2().intValue(), null);

                StrikeTO orDefault = strikeTOMap.getOrDefault(trade.getStockSymbol(), null);

                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

                boolean isScWithPriceDecreasing = false;
                if(orDefault != null) {
                    isScWithPriceDecreasing = currentStrike.getCeOiChg() < orDefault.getCeOiChg() &&
                            currentStrike.getPeOiChg() > orDefault.getPeOiChg() &&
                            currentStrike.getCurPrice() < orDefault.getCurPrice();
                }

                if(currentStrike.getCeOiChg() > 0) {
                    existStocks.append("<tr>")
                            .append("<td>").append(trade.getStockSymbol()).append("</td>")
                            .append("<td>").append(String.format("%.2f", trade.getOiChgPer())).append("%</td>")
                            .append("<td>").append(String.format("%.2f", trade.getLtpChgPer())).append("%</td>")
                            .append("<td>").append(trade.getEntry2().intValue()).append("</td>")
                            .append("<td>").append(trade.getFetchTime()).append("</td>")
                            .append("<td>").append(strike.getCeOiChg()).append("</td>")
                            .append("<td>").append(strike.getPeOiChg()).append("</td>")
                            .append("<td>").append(strike.getCurPrice()).append("</td>")
                            .append("<td>").append(currentStrike.getTime()).append("</td>")
                            .append("<td>").append(currentStrike.getCeOiChg()).append("</td>")
                            .append("<td>").append(currentStrike.getPeOiChg()).append("</td>")
                            .append("<td>").append(currentStrike.getCurPrice()).append("</td>")
                            .append("</tr>");
                }

                if (currentStrike != null && strike != null && isScWithPriceDecreasing &&
                        currentStrike.getCeOiChg() < strike.getCeOiChg() &&
                        currentStrike.getPeOiChg() > strike.getPeOiChg()) {
                    currentStrike.setTime(time);
                    tableContent.append("<tr>")
                            .append("<td>").append(trade.getStockSymbol()).append("</td>")
                            .append("<td>").append(String.format("%.2f", trade.getOiChgPer())).append("%</td>")
                            .append("<td>").append(String.format("%.2f", trade.getLtpChgPer())).append("%</td>")
                            .append("<td>").append(trade.getEntry2().intValue()).append("</td>")
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
                        tableContent.append("<td>").append(isScWithPriceDecreasing ? "Yes" : "No").append("</td>");
                    } else {
                        tableContent.append("<td>N/A</td>");
                    }

                }
                if(currentStrike != null) {
                    currentStrike.setTime(time);
                    strikeTOMap.put(trade.getStockSymbol(), currentStrike);
                }

            } catch (Exception e) {
                System.out.println("Error while checking option chain for " + trade.getStockSymbol());
            }
        }

        tableContent.append("</table>");
        existStocks.append("</table>");

        if(existStocks.toString().contains("<td>")) {
            mailService.sendMail(
                    " Exist stocks if you have taken " + properties.getStockDate(),
                    "<html><body>" +
                            "<h3>Option Chain Verification Results - Existing Stocks</h3>" +
                            existStocks.toString() +
                            "</body></html>"
            );
        }

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


    private Properties buildProperties() {
        Properties properties = new Properties();
        properties.setStockDate(LocalDate.now().toString());
        String startTime = "09:15:00";
        String endTime = "15:30:00";
        properties.setStartTime(startTime);
        properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));
        properties.setEndTime(endTime);
        return properties;
    }

}


