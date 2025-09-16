package com.stocks.controller;

import com.stocks.dto.Properties;
import com.stocks.dto.TradeSetupTO;
import com.stocks.mail.MarketMoversMailService;
import com.stocks.repository.TradeSetupManager;
import com.stocks.service.DayHighLowBreakService;
import com.stocks.service.DayHighLowService;
import com.stocks.service.MarketMovers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
public class MarketMoverController {

	@Autowired
	DayHighLowBreakService dayHighLowBreakService;
	@Autowired
	MarketMovers marketMovers;
	@Autowired
	MarketMoversMailService marketMoversMailService;
	@Autowired
	TradeSetupManager tradeSetupManager;



	@GetMapping("/test-market-movers-gainers")
	public String marketMoversGainers(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
	                                  @RequestParam(name = "interval", required = false, defaultValue = "0") Integer interval,
	                                  @RequestParam(name = "strategy", required = false, defaultValue = "") String strategy,
	                                  @RequestParam(name = "stockName", required = false, defaultValue = "") String stockName,
                                      @RequestParam(name = "modifyStopLoss", required = false) boolean modifyStopLoss) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setInterval(interval);
		properties.setStockName(stockName);
		properties.setStrategy(strategy);
        properties.setModifyStopLoss(modifyStopLoss);
		List<TradeSetupTO> trades = marketMovers.testPositiveMarketMovers(properties);
		if (trades == null || trades.isEmpty()) {
			return "No records found";
		}
		String data = marketMoversMailService.beautifyTestResults(trades);
		try {
			saveTradesToCsv(trades, "market-movers-gainers.csv");
		} catch (IOException e) {
			log.error("Error saving trades to CSV: ", e);
		}
		marketMoversMailService.sendMail(data, properties, "Back Test Report");
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
//		marketMoversMailService.sendMail(data, properties, "Market Movers Report");
		return data;
	}

	@GetMapping("/option-chain-data")
	public String optionChainData(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
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
		List<TradeSetupTO> trades = marketMovers.optionChainData(properties);
		if (trades == null || trades.isEmpty()) {
			return "No records found";
		}
		String data = marketMoversMailService.beautifyTestResults(trades);
//		marketMoversMailService.sendMail(data, properties, "Option Chain Report " + properties.getInterval() + " mins");
		return data;
	}


	public void saveTradesToCsv(List<TradeSetupTO> trades, String filePath) throws IOException, IOException {
		if (trades == null || trades.isEmpty()) return;
		// Create or overwrite the file
		try (java.io.FileWriter writer = new java.io.FileWriter(filePath, true)) {
			// Write header
//			writer.append("Stock,OI,LTP,Status,Date,Time,Entry1,Entry1Time,Entry2,Entry2Time,Target1,Target1Time,Target2,Target2Time,StopLoss,StopLossTime,Notes,Strategy,Type\n");
			for (TradeSetupTO trade : trades) {
				writer.append(safe(trade.getStockSymbol())).append(",");
				writer.append(format(trade.getOiChgPer())).append(",");
				writer.append(format(trade.getLtpChgPer())).append(",");
				writer.append(safe(trade.getStatus())).append(",");
				writer.append(safe(trade.getStockDate())).append(",");
				writer.append(safe(trade.getFetchTime())).append(",");
				writer.append(format(trade.getEntry1())).append(",");
				writer.append(safe(trade.getEntry1Time())).append(",");
				writer.append(format(trade.getEntry2())).append(",");
				writer.append(safe(trade.getEntry2Time())).append(",");
				writer.append(format(trade.getTarget1())).append(",");
				writer.append(safe(trade.getTarget1Time())).append(",");
				writer.append(format(trade.getTarget2())).append(",");
				writer.append(safe(trade.getTarget2Time())).append(",");
				writer.append(format(trade.getStopLoss1())).append(",");
				writer.append(safe(trade.getStopLoss1Time())).append(",");
				writer.append(safe(trade.getTradeNotes())).append(",");
				writer.append(safe(trade.getStrategy())).append(",");
				writer.append(safe(trade.getType())).append("\n");
			}
		}
	}

	@GetMapping("/getTradeSetups")
	public String getTradeSetups(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate) {
		List<TradeSetupTO> tradeSetupByDate = tradeSetupManager.findTradeSetupByDate(stockDate);
		String data = marketMoversMailService.beautifyTestResults(tradeSetupByDate);
//		marketMoversMailService.sendMail(data, properties, "Market Movers Report");
		return data;
	}

	@GetMapping("/verifyTradeSetups")
	public String checkOptionChainForTradeSetUpStocks(@RequestParam(name = "stockDate", required = false, defaultValue = "") String stockDate,
													  @RequestParam(name = "strategy", required = false, defaultValue = "c2") String strategy) {
		Properties properties = new Properties();
		properties.setStockDate(stockDate);
		properties.setStrategy(strategy);
		dayHighLowBreakService.checkOptionChainForTradeSetUpStocks(properties);
		return "success";
	}

	private String format(Number n) {
		return n != null ? String.format("%.2f", n) : "";
	}

	private String safe(String s) {
		return s != null ? s.replace(",", " ") : "";
	}
}
