package com.stocks.service;

import com.stocks.dto.ApiResponse;
import com.stocks.dto.StockProfitResult;
import com.stocks.dto.StockResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestingResultsService {

    @Autowired
    private IOPulseService ioPulseService;

    @Value(("${exitTime}"))
    private String exitTime;

    private static final Logger log = LoggerFactory.getLogger(TestingResultsService.class);

    public void testingResults(List<StockResponse> emailList, String selectedDate) {
        // Split into two lists
        List<StockResponse> nsList = emailList.stream()
                .filter(email -> email.getStockType().equals("N"))
                .collect(Collectors.toList());

        List<StockResponse> psList = emailList.stream()
                .filter(email -> email.getStockType().equals("P"))
                .collect(Collectors.toList());

        double ntotal = 0;
        double nprofit = 0;
        double ptotal = 0;
        double pprofit = 0;
        int nsCount = nsList.size();
        int psCount = psList.size();
        for (StockResponse stock : nsList) {
            String stockName = stock.getStock();
            double stopLoss = stock.getStopLoss();
            double price = stock.getCurrentPrice();
            double sell = price * 10;
            ResponseEntity<ApiResponse> response = ioPulseService.sendRequest(stockName, selectedDate);
            ApiResponse apiResponse = response.getBody();
            double stockProfit = 0;
            for (ApiResponse.Data data : apiResponse.getData()) {
                if (data.getTime().equals(exitTime)) {
                    double buy = data.getClose() * 10;
                    stockProfit = sell - buy;
                    StockProfitResult result = new StockProfitResult(stockProfit, sell, buy, exitTime, stock.getEndTime());
                    stock.setStockProfitResult(result);
                    break;
                }
            }
            ntotal = ntotal + sell;
            nprofit = nprofit + stockProfit;
        }

        for (StockResponse stock : psList) {
            String stockName = stock.getStock();
            double price = stock.getCurrentPrice();
            double buy = price * 10;

            ResponseEntity<ApiResponse> response = ioPulseService.sendRequest(stockName, selectedDate);
            ApiResponse apiResponse = response.getBody();
            double stockProfit = 0;

            for (ApiResponse.Data data : apiResponse.getData()) {
                if (data.getTime().equals(exitTime)) {
                    double sell = data.getClose() * 10;
                    stockProfit = sell - buy;
                    StockProfitResult result = new StockProfitResult(stockProfit, sell, buy, stock.getEndTime(), exitTime);
                    stock.setStockProfitResult(result);
                    break;
                }
            }
            ptotal = ptotal + buy;
            pprofit = pprofit + stockProfit;

        }
        log.info(nsCount + " negative stocks total amt invested " + ntotal + " and profit " + nprofit);
        log.info(psCount + " positive stocks total amt invested " + ptotal + " and profit " + pprofit);
    }
}