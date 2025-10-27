package com.stocks.controller;

import com.stocks.dto.Properties;
import com.stocks.dto.TradeSetupTO;
import com.stocks.mail.MarketMoversMailService;
import com.stocks.service.NewFutureAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class FutureAnalysisController {

	@Autowired
	private NewFutureAnalysis newFutureAnalysis;
	@Autowired
	MarketMoversMailService marketMoversMailService;

	@GetMapping("/future-analysis")
	public String callApi(
			@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
			@RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
			@RequestParam(name = "env", required = false, defaultValue = "") String env,
			@RequestParam(name = "stockName", required = false, defaultValue = "") String stockName) {

		long startTime = System.currentTimeMillis();
		log.info("API call started - stockDate: {}, interval: {}, env: {}, stockName: {}",
				stockDate, interval, env, stockName);

		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		properties.setStartTime("09:15");
		properties.setEnv(env);

		long serviceStartTime = System.currentTimeMillis();
		List<TradeSetupTO> list1 = newFutureAnalysis.futureAnalysis(properties);
		long serviceEndTime = System.currentTimeMillis();
		log.info("MarketDataService.callApi completed in {} ms, found {} results",
				(serviceEndTime - serviceStartTime), list1.size());

		String data = "";
		long mailStartTime = System.currentTimeMillis();
		try {
			if (!list1.isEmpty()) {
				data = marketMoversMailService.beautifyResults(list1);
				marketMoversMailService.sendMail(data, properties, "Market Movers Report");
			}
		} catch (Exception e) {
			log.error("error in beautifyResults: ", e);
		}
		long mailEndTime = System.currentTimeMillis();

		long totalTime = System.currentTimeMillis() - startTime;
		log.info("API call completed - Total time: {} ms, Mail processing: {} ms, Results: {} trades",
				totalTime, (mailEndTime - mailStartTime), list1.size());

		// Provide informative response when no data is found
		if (list1.isEmpty() && data.isEmpty()) {
			data = "No trade setups found.";
		}

		System.gc();
		return data;
	}
}
