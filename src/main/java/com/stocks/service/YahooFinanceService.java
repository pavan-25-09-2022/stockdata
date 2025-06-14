package com.stocks.service;

import com.stocks.dto.FutureAnalysis;
import com.stocks.dto.HistoricalQuote;
import com.stocks.dto.Properties;
import com.stocks.enumaration.QueryInterval;
import com.stocks.utils.CalenderUtil;
import com.stocks.utils.DateUtil;
import com.stocks.yahoo.HistQuotesQuery2V8RequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class YahooFinanceService {


    @Autowired
    MailService mailService;

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceService.class);

    @Value("${date}")
    private String date;

    public Map<String, Map<String, HistoricalQuote>> getStocksDetails(Properties properties) throws IOException {

        String stockDate = properties.getStockDate();
        Calendar from = null;
        Calendar to = null;

        String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() : date != null ? date : LocalDateTime.now().toString();
        if (selectedDate != null) {
            Date dateFromString = DateUtil.getDateFromString(selectedDate);
            from = CalenderUtil.startHoursFromDate(dateFromString);
            to = CalenderUtil.endHoursFromDate(dateFromString);
        } else {
            from = CalenderUtil.start();
            to = CalenderUtil.end();
        }

        // Path to the file containing the stock list
        String filePath = "src/main/resources/stocksEodList.txt";

        // Read all lines from the file into a List
        List<String> stockList;
        if (properties.getStockName() != null) {
            stockList = Arrays.asList(properties.getStockName().split(","));
        } else {
            try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
                stockList = lines.collect(Collectors.toList());
            } catch (IOException e) {
                log.error("Error reading file: " + e.getMessage());
                return null;
            }
        }

        Map<String, Map<String, HistoricalQuote>> stockDetails = new HashMap<>();

        for (String stock : stockList) {

            String stockName = stock + ".NS";
            QueryInterval queryInterval = properties.getInterval() == 0 ? QueryInterval.FIFTEEN_MINS : QueryInterval.FIVE_MINS;
            HistQuotesQuery2V8RequestImpl impl1 = new HistQuotesQuery2V8RequestImpl(stockName, from, to, queryInterval);

            List<HistoricalQuote> completeResult = impl1.getCompleteResult();

            Map<String, HistoricalQuote> historicalQuoteMap = new LinkedHashMap<>();
            for (HistoricalQuote quote : completeResult) {

                String duration = LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()).toLocalTime() +
                        "-" + LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()).toLocalTime().plusMinutes(5);
                historicalQuoteMap.put(duration, quote);
            }
            stockDetails.put(stock, historicalQuoteMap);

        }
        return stockDetails;
    }

    public void combineFutureAndSpotDetails(Properties properties, Map<String, Map<String, FutureAnalysis>> stocksFutureAnalysisMap) throws IOException {

        String stockDate = properties.getStockDate();
        Calendar from = null;
        Calendar to = null;

        String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() : date != null ? date : LocalDateTime.now().toString();
        if (selectedDate != null) {
            Date dateFromString = DateUtil.getDateFromString(selectedDate);
            from = CalenderUtil.startHoursFromDate(dateFromString);
            to = CalenderUtil.endHoursFromDate(dateFromString);
        } else {
            from = CalenderUtil.start();
            to = CalenderUtil.end();
        }

        // Path to the file containing the stock list
        String filePath = "src/main/resources/stocksEodList.txt";

        // Read all lines from the file into a List
        List<String> stockList;
        if (properties.getStockName() != null && !properties.getStockName().isEmpty()) {
            stockList = Arrays.asList(properties.getStockName().split(","));
        } else {
            try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
                stockList = lines.collect(Collectors.toList());
            } catch (IOException e) {
                log.error("Error reading file: " + e.getMessage());
                return;
            }
        }

        for (String stock : stockList) {

            Map<String, FutureAnalysis> futureAnalysisMap = stocksFutureAnalysisMap.get(stock);
            if (futureAnalysisMap == null) {
                continue;
            }
            String stockName = stock + ".NS";

            if ("BANKNIFTY".equals(stock)) {
                stockName = "^NSEBANK";
            }
            if ("NIFTY".equals(stock)) {
                stockName = "^NSEI";
            }
            QueryInterval queryInterval = QueryInterval.getInstance(properties.getInterval() + "m");
            HistQuotesQuery2V8RequestImpl impl1 = new HistQuotesQuery2V8RequestImpl(stockName, from, to, queryInterval);

            List<HistoricalQuote> completeResult = impl1.getCompleteResult();

            for (HistoricalQuote quote : completeResult) {
                String duration = LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()) +
                        "-" + LocalDateTime.ofInstant(quote.getDate().toInstant(), ZoneId.systemDefault()).plusMinutes(properties.getInterval());
                FutureAnalysis futureAnalysis = futureAnalysisMap.get(duration);
                if (futureAnalysis != null) {
                    futureAnalysis.setHistoricalQuote(quote);
                }
            }
        }
    }
}
