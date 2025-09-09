package com.stocks.loader;

import com.stocks.dto.TradeSetupTO;
import com.stocks.repository.TradeSetupManager;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

//@Component
public class TradeSetupLoader {
	//	@Autowired
	private TradeSetupManager tradeSetupRepository;

	@PostConstruct
	public void loadCsvData() throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader("market-movers-gainers.csv"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(",");
				TradeSetupTO setup = new TradeSetupTO();
				setup.setStockSymbol(parts[0]);
				setup.setOiChgPer(Double.parseDouble(parts[1]));
				setup.setLtpChgPer(Double.parseDouble(parts[2]));
				setup.setStockDate(parts[4]);
				setup.setFetchTime(parts[5]);
				setup.setEntry1(Double.parseDouble(parts[6]));
				setup.setEntry2(Double.parseDouble(parts[8]));
				setup.setTarget1(Double.parseDouble(parts[10]));
				setup.setTarget2(Double.parseDouble(parts[12]));
				setup.setStopLoss1(Double.parseDouble(parts[14]));
				setup.setStrategy(parts[17]);
				setup.setType(parts[18]);
				tradeSetupRepository.saveTradeSetup(setup);
			}
		}
	}
}