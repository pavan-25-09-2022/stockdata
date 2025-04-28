package com.stocks.controller;

import com.stocks.dto.StockResponse;
import com.stocks.mail.Mail;
import com.stocks.service.DayHighLowService;
import com.stocks.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApiController {

    @Autowired
    private MarketDataService apiService;

    @Autowired
    private DayHighLowService dayHighLowService;

    @Autowired
    private Mail mailService;

    @GetMapping("/call-api")
    public String callApi(@RequestParam (name = "minutes", required = false, defaultValue = "0") Integer minutes,
                          @RequestParam (name = "fetchAll", required = false) boolean fetchAll) {
        List<StockResponse> list = apiService.callApi(minutes, fetchAll);
        if (list == null || list.isEmpty()) {
            return "No data found";
        }
        String data = mailService.beautifyResults(list);
        mailService.sendMail(data);
        System.gc();
        return data;
    }

    @GetMapping("/apiDayHighLow")
    public String apiDayHighLow() {
        List<StockResponse> list = dayHighLowService.dayHighLow();
        if (list == null || list.isEmpty()) {
            return "No data found";
        }
        String data = mailService.beautifyResults(list);
        mailService.sendMail(data);
        System.gc();
        return data;
    }
}