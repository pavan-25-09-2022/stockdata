package com.stocks.controller;

import com.stocks.dto.Properties;
import com.stocks.dto.TradeSetupTO;
import com.stocks.mail.MarketMoversMailService;
import com.stocks.service.MarketMovers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MarketMoverController {

	@Autowired
	MarketMovers marketMovers;
	@Autowired
	MarketMoversMailService marketMoversMailService;

	@GetMapping("/test-market-movers-gainers")
	public String marketMoversGainers(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                                  @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                                  @RequestParam(name = "strategy", required = false, defaultValue = "") String strategy,
	                                  @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		properties.setStrategy(strategy);
		List<TradeSetupTO> trades = marketMovers.testPositiveMarketMovers(properties);
		if (trades == null || trades.isEmpty()) {
			return "No records found";
		}
		String data = marketMoversMailService.beautifyTestResults(trades);
		marketMoversMailService.sendMail(data, properties);
		return data;
	}

	@GetMapping("/market-movers-gainers-based-on-volume")
	public String marketMoversGainersBasedOnVolume(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                                  @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                                  @RequestParam(name = "strategy", required = false, defaultValue = "") String strategy,
												   @RequestParam(name = "env", required = false, defaultValue = "") String env,
	                                  @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		properties.setStrategy(strategy);
		properties.setEnv(env);
		List<TradeSetupTO> trades = marketMovers.marketMoverDetailsBasedOnVolume(properties, "G");
		if (trades == null || trades.isEmpty()) {
			return "No records found";
		}
		String data = marketMoversMailService.beautifyTestResults(trades);
		marketMoversMailService.sendMail(data, properties);
		return data;
	}

}
