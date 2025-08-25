package com.stocks.controller;

import com.stocks.dto.Properties;
import com.stocks.dto.TradeSetupTO;
import com.stocks.dto.TrendingOiResponse;
import com.stocks.entity.TrendingOiEntity;
import com.stocks.service.TrendingOIService;
import com.stocks.utils.FormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class TrendingOiController {

    @Autowired
    TrendingOIService trendingOIService;

    @GetMapping("/trendingOiDetails")
    public List<TrendingOiEntity> trendingOiDetails(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
                                                   @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
                                                   @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {
        Properties properties = new Properties();
        properties.setStockDate(stockDate);
        properties.setInterval(interval);
        properties.setStockName(stockName);
        properties.setExpiryDate(FormatUtil.getMonthExpiry(properties.getStockDate()));
        return trendingOIService.fetchTrendingOIData(properties);
    }
}
