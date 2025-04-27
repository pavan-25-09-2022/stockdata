package com.stocks.controller;

import com.stocks.dto.StockResponse;
import com.stocks.mail.Mail;
import com.stocks.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ApiController {

    @Autowired
    private MarketDataService apiService;

    @Autowired
    private Mail mailService;

    @GetMapping("/call-api")
    public String callApi() {
        List<StockResponse> list = apiService.callApi();
        if (list == null || list.isEmpty()) {
            return "No data found";
        }
        String data = beautifyResults(list);
//        mailService.sendMail(data);
        return data;
    }

    private String beautifyResults(List<StockResponse> list) {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><body>");
        htmlContent.append("<h2>Stock Report</h2>");
        htmlContent.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        htmlContent.append("<tr>")
                .append("<th>P</th>")
                .append("<th>Stock</th>")
                .append("<th>Cur Price</th>")
                .append("<th>Time</th>")
                .append("<th>OI Interpretation</th>")
                .append("<th>Stop Loss</th>")
                .append("<th>EOD OI</th>");
        if (list.get(0).getStockProfitResult() != null) {
            htmlContent.append("<th> profit</th>")
                    .append("<th> sellPrice</th>")
                    .append("<th> buyPrice</th>")
                    .append("<th> buyTime</th>")
                    .append("<th> sellTime</th>");
        }
        htmlContent.append("</tr>");

        // Group rows by stock type
        List<StockResponse> nStocks = list.stream()
                .filter(stock -> "N".equals(stock.getStockType()))
                .sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
                .collect(Collectors.toList());

        List<StockResponse> pStocks = list.stream()
                .filter(stock -> "P".equals(stock.getStockType()))
                .sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
                .collect(Collectors.toList());

// Append rows for N stocks
        Map<String, List<StockResponse>> nsStocks = nStocks.stream()
                .collect(Collectors.groupingBy(StockResponse::getOiInterpretation));

        for (Map.Entry<String, List<StockResponse>> entry : nsStocks.entrySet()) {
            List<StockResponse> stocks = entry.getValue();
            for (StockResponse stock : stocks) {
//                if (stock.getPriority() == 0) {
//                    continue;
//                }
                htmlContent.append("<tr style='background-color: #ffcccc;'>")
                        .append("<td>").append(stock.getPriority()).append("</td>")
                        .append("<td>").append(stock.getStock()).append("</td>")
                        .append("<td>").append(stock.getCurrentPrice()).append("</td>")
                        .append("<td>").append(stock.getTime()).append("</td>")
                        .append("<td>").append(stock.getOiInterpretation()).append("</td>")
                        .append("<td>").append(stock.getStopLoss()).append("</td>")
                        .append("<td>").append(stock.getEodData()).append("</td>");
                if (stock.getStockProfitResult() != null) {
                    htmlContent.append("<td style='background-color: ")
                            .append(stock.getStockProfitResult().getProfit() > 0 ? "#ccffcc" : "#ffcccc")
                            .append(";'>")
                            .append(String.format("%.2f", stock.getStockProfitResult().getProfit()))
                            .append("</td>")
                            .append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getSellPrice())).append("</td>")
                            .append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getBuyPrice())).append("</td>")
                            .append("<td>").append(stock.getStockProfitResult().getBuyTime()).append("</td>")
                            .append("<td>").append(stock.getStockProfitResult().getSellTime()).append("</td>");
                }
                htmlContent.append("</tr>");
            }
            // Add one empty row after each group
            htmlContent.append("<tr><td colspan='10'>&nbsp;</td></tr>");
        }


// Append rows for P stocks
        Map<String, List<StockResponse>> psStocks = pStocks.stream()
                .collect(Collectors.groupingBy(StockResponse::getOiInterpretation));

        for (Map.Entry<String, List<StockResponse>> entry : psStocks.entrySet()) {
            List<StockResponse> stocks = entry.getValue();
            for (StockResponse stock : stocks) {
                if (stock.getPriority() == 0) {
                    continue;
                }
                htmlContent.append("<tr style='background-color: #ccffcc;'>")
                        .append("<td>").append(stock.getPriority()).append("</td>")
                        .append("<td>").append(stock.getStock()).append("</td>")
                        .append("<td>").append(stock.getCurrentPrice()).append("</td>")
                        .append("<td>").append(stock.getTime()).append("</td>")
                        .append("<td>").append(stock.getOiInterpretation()).append("</td>")
                        .append("<td>").append(stock.getStopLoss()).append("</td>")
                        .append("<td>").append(stock.getEodData()).append("</td>");
                if (stock.getStockProfitResult() != null) {
                    htmlContent.append("<td style='background-color: ")
                            .append(stock.getStockProfitResult().getProfit() > 0 ? "#ccffcc" : "#ffcccc")
                            .append(";'>")
                            .append(String.format("%.2f", stock.getStockProfitResult().getProfit()))
                            .append("</td>")
                            .append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getSellPrice())).append("</td>")
                            .append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getBuyPrice())).append("</td>")
                            .append("<td>").append(stock.getStockProfitResult().getBuyTime()).append("</td>")
                            .append("<td>").append(stock.getStockProfitResult().getSellTime()).append("</td>");
                }
                htmlContent.append("</tr>");
            }
            // Add one empty row after each group
            htmlContent.append("<tr><td colspan='10'>&nbsp;</td></tr>");
        }
        htmlContent.append("</table>");
        htmlContent.append("</body></html>");
        return htmlContent.toString();
    }
}