package com.stocks.service;

import com.stocks.dto.ApiResponse;
import com.stocks.dto.Properties;
import com.stocks.dto.StockProfitResult;
import com.stocks.dto.StockResponse;
import com.stocks.utils.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestingResultsService {

    @Autowired
    private IOPulseService ioPulseService;

    @Value(("${exitTime}"))
    private String exitTime;

    @Autowired
    private OptionChainService optionChainService;

    private static final Logger log = LoggerFactory.getLogger(TestingResultsService.class);

    public void testingResults(List<StockResponse> emailList, Properties properties) {
        // Split into two lists
        List<StockResponse> nsList = emailList.stream()
                .filter(email -> email.getStockType().equals("N"))
                .collect(Collectors.toList());

        List<StockResponse> psList = emailList.stream()
                .filter(email -> email.getStockType().equals("P"))
                .collect(Collectors.toList());

        int amtInvested = properties.getAmtInvested();

        double ntotal = 0;
        double nprofit = 0;
        double ptotal = 0;
        double pprofit = 0;
        int nsCount = nsList.size();
        int psCount = psList.size();
        for (StockResponse stock : nsList) {
            try {
                String stockName = stock.getStock();
                double stopLoss = stock.getStopLoss();
                double price = stock.getCurrentPrice();
                int v = (int) (amtInvested / price);
                double sell = price * v;
                ApiResponse apiResponse= ioPulseService.sendRequest(properties, stockName);
//                ApiResponse apiResponse = response.getBody();
                double stockProfit = 0;
                LocalTime exitT = null;
                if(properties.getExitMins()==0) {
                    String exit1 = optionChainService.calculateExitTime(stock, properties);
                    exitT = LocalTime.parse(exit1);
                }

                for (int i = 0; i < apiResponse.getData().size(); i++) {
                    if (i == 0) {
                        continue;
                    }
                    ApiResponse.Data data = apiResponse.getData().get(i);
                    LocalTime current = LocalTime.parse((data.getTime()));
                if (properties.getExitMins() > 0) {
                    exitT = stock.getEndTime().plusMinutes(properties.getExitMins());
                }
                    if (current.isAfter(exitT)) {
                        double buy = data.getClose() * v;
                        stockProfit = sell - buy;
                        StockProfitResult result = new StockProfitResult(stockProfit, sell, buy, exitT.toString(), FormatUtil.formatTimeHHmm(stock.getEndTime()));
                        stock.setStockProfitResult(result);
                        break;
                    }
                }
                ntotal = ntotal + sell;
                nprofit = nprofit + stockProfit;
            }catch (Exception e){
                log.error("Error processing stock " + stock.getStock() + ": " + e.getMessage());
            }
        }

        for (StockResponse stock : psList) {
            try{
            String stockName = stock.getStock();
            double price = stock.getCurrentPrice();
            int v = (int) (amtInvested / price);
            double buy = price * v;

                ApiResponse apiResponse = ioPulseService.sendRequest(properties, stockName);
//            ApiResponse apiResponse = response.getBody();
            double stockProfit = 0;
                LocalTime exitT = null;
                if(properties.getExitMins()==0) {
                    String exit1 = optionChainService.calculateExitTime(stock, properties);
                    exitT = LocalTime.parse(exit1);
                }
            for (int i = 0; i < apiResponse.getData().size(); i++) {
                if (i == 0) {
                    continue;
                }
                ApiResponse.Data data = apiResponse.getData().get(i);
                LocalTime current = LocalTime.parse((data.getTime()));
                if (properties.getExitMins() > 0) {
                    exitT = stock.getEndTime().plusMinutes(properties.getExitMins());
                }
                if (current.isAfter(exitT)) {
                    double sell = data.getClose() * v;
                    stockProfit = sell - buy;
                    StockProfitResult result = new StockProfitResult(stockProfit, sell, buy, FormatUtil.formatTimeHHmm(stock.getEndTime()), exitT.toString());
                    stock.setStockProfitResult(result);
                    break;
                }
            }
            ptotal = ptotal + buy;
            pprofit = pprofit + stockProfit;
            }catch (Exception e){
                log.error("Error processing stock " + stock.getStock() + ": " + e.getMessage());
            }
        }
        log.info(nsCount + " negative stocks total amt invested " + ntotal + " and profit " + nprofit);
        log.info(psCount + " positive stocks total amt invested " + ptotal + " and profit " + pprofit);
    }
}