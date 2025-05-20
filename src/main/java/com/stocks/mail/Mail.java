package com.stocks.mail;

import com.stocks.dto.Properties;
import com.stocks.dto.StockResponse;
import com.stocks.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class Mail {

    @Autowired
    private MailService mailService;

    public void sendMail(String data, Properties properties) {
        // Send email
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        if(properties.getStockDate() != null && !properties.getStockDate().isEmpty()){
            currentDate = LocalDate.parse(properties.getStockDate());
        }
        String formattedDate = currentDate.format(formatter);
        String sub;
        if("test".equalsIgnoreCase(properties.getEnv())){
            sub = "Test Stock Report " + formattedDate;
            if(properties.getExitMins() >0){
                sub = sub + " - "+ properties.getExitMins() + " Exit Mins";
            }
        } else {
            sub =  "Stock Report" + formattedDate;
        }
        mailService.sendMail(sub, data);

    }

    public void sendTrendsMail(String data, Properties properties) {
        // Send email
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        if(properties.getStockDate() != null && !properties.getStockDate().isEmpty()){
            currentDate = LocalDate.parse(properties.getStockDate());
        }
        String formattedDate = currentDate.format(formatter);
        String sub;
        if("test".equalsIgnoreCase(properties.getEnv())){
            sub = "Test Stock Trends Report " + formattedDate;
            if(properties.getExitMins() >0){
                sub = sub + " - "+ properties.getExitMins() + " Exit Mins";
            }
        } else {
            sub =  "Stock Trends Report" + formattedDate;
        }
        mailService.sendMail(sub, data);

    }
    public String beautifyResults(List<StockResponse> list, Properties properties) {
        return mailService.beautifyResults(list, properties);
    }

    public String beautifyTrendResults(List<StockResponse> list, Properties properties) {
        return mailService.beautifyTrendResults(list, properties);
    }

    public String beautifyOptChnResults(List<StockResponse> stockResponse) {
        return mailService.beautifyOptChnResults(stockResponse);
    }

    public void sendOptMail(String data) {
        // Send email
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = currentDate.format(formatter);
        mailService.sendMail("Option Chain Report " + formattedDate, data);
    }

    public void sendTrendMail(String data1, Properties properties) {
        // Send email
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        if(properties.getStockDate() != null && !properties.getStockDate().isEmpty()){
            currentDate = LocalDate.parse(properties.getStockDate());
        }
        String formattedDate = currentDate.format(formatter);
        String sub =  "Stock Trend Report" + formattedDate;

        mailService.sendMail(sub, data1);
    }
}
