package com.stocks.scheduler;

import com.stocks.dto.Properties;
import com.stocks.dto.TradeSetupTO;
import com.stocks.mail.MarketMoversMailService;
import com.stocks.service.EodOptionChainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class EodOptionChainScheduler {

	@Autowired
	private EodOptionChainService eodOptionChainService;
	@Autowired
	private MarketMoversMailService marketMoversMailService;

	@Scheduled(cron = "10 */5 9-13 * * *") // Runs every 5 minutes from 9:15 to 13:35
	public void callApi() {
		log.info("Scheduler started API stocks");
		Properties properties = new Properties();
		properties.setStockDate(LocalDate.now().toString());
		properties.setInterval(15);
		List<TradeSetupTO> list = eodOptionChainService.validateEodOptionChain(properties);
		if (list.isEmpty()) {
			log.info("No records found");
			return;
		}
		String data = marketMoversMailService.beautifyResults(list);
		marketMoversMailService.sendMail(data, properties, "C7 Criteria Report");
		System.gc();
		log.info("Scheduler finished " + list.size());
	}

}
