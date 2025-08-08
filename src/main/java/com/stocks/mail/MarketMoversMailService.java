package com.stocks.mail;

import com.stocks.dto.Properties;
import com.stocks.dto.TradeSetupTO;
import com.stocks.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MarketMoversMailService {

	@Autowired
	private MailService mailService;

	public String beautifyResults(List<TradeSetupTO> trades) {
		if (trades == null || trades.isEmpty()) return "No trades found.";
		StringBuilder sb = new StringBuilder();
		sb.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;'>");
		sb.append("<tr>")
				.append("<th>Stock</th>")
				.append("<th>Date</th>")
				.append("<th>Time</th>")
				.append("<th>Entry1</th>")
				.append("<th>Entry2</th>")
				.append("<th>Target1</th>")
				.append("<th>Target2</th>")
				.append("<th>Stop Loss</th>")
				.append("<th>Notes</th>")
				.append("<th>Strategy</th>")
				.append("<th>Type</th>")
				.append("</tr>");
		for (TradeSetupTO trade : trades) {
			sb.append("<tr>")
					.append("<td>").append(trade.getStockSymbol()).append("</td>")
					.append("<td>").append(trade.getStockDate()).append("</td>")
					.append("<td>").append(trade.getFetchTime()).append("</td>")
					.append("<td>").append(trade.getEntry1() != null ? String.format("%.2f", trade.getEntry1()) : "").append("</td>")
					.append("<td>").append(trade.getEntry2() != null ? String.format("%.2f", trade.getEntry2()) : "").append("</td>")
					.append("<td>").append(trade.getTarget1() != null ? String.format("%.2f", trade.getTarget1()) : "").append("</td>")
					.append("<td>").append(trade.getTarget2() != null ? String.format("%.2f", trade.getTarget2()) : "").append("</td>")
					.append("<td>").append(trade.getStopLoss1() != null ? String.format("%.2f", trade.getStopLoss1()) : "").append("</td>")
					.append("<td>").append(trade.getTradeNotes() != null ? trade.getTradeNotes() : "").append("</td>")
					.append("<td>").append(trade.getStrategy() != null ? trade.getStrategy() : "").append("</td>")
					.append("<td>").append(trade.getType() != null ? trade.getType() : "").append("</td>")
					.append("</tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}

	public void sendMail(String data, Properties properties) {
		// Send email
		LocalDate currentDate = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		if (properties.getStockDate() != null && !properties.getStockDate().isEmpty()) {
			currentDate = LocalDate.parse(properties.getStockDate());
		}
		String formattedDate = currentDate.format(formatter);
		String sub;
		if ("test".equalsIgnoreCase(properties.getEnv())) {
			sub = "Market Movers Report " + formattedDate;
			if (properties.getExitMins() > 0) {
				sub = sub + " - " + properties.getExitMins() + " Exit Mins";
			}
		} else {
			sub = "Market Movers Report " + formattedDate;
		}
		mailService.sendMail(sub, data);

	}
}
