package com.stocks.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketMovers {

    @Autowired
    private IOPulseService ioPulseService;

    public void getMarketMovers() {

    }
}
