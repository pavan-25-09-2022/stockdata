package com.stocks.mail;

import com.stocks.dto.Properties;
import com.stocks.dto.TradeSetupTO;
import com.stocks.entity.StrikeSetupEntity;
import com.stocks.entity.TradeSetupEntity;
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

	public String beautifyTradeSetupResults(List<TradeSetupEntity> trades) {
		if (trades == null || trades.isEmpty()) return "No trades found.";
		StringBuilder sb = new StringBuilder();
		sb.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;'>");
		sb.append("<tr>")
				.append("<th>ID</th>")
				.append("<th>Stock</th>")
				.append("<th>Date</th>")
				.append("<th>Entry1</th>")
				.append("<th>Entry2</th>")
				.append("<th>Target1</th>")
				.append("<th>Target2</th>")
				.append("<th>Stop Loss</th>")
				.append("<th>Notes</th>")
				.append("<th>Strategy</th>")
				.append("<th>Type</th>")
				.append("</tr>");
		for (TradeSetupEntity trade : trades) {
			sb.append("<tr>")
					.append("<td>").append(trade.getId()).append("</td>")
					.append("<td>").append(trade.getStockSymbol()).append("</td>")
					.append("<td>").append(trade.getStockDate()).append("</td>")
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

	public String beautifyStrikeSetupResults(List<StrikeSetupEntity> strikes) {
		if (strikes == null || strikes.isEmpty()) return "No strike setups found.";
		StringBuilder sb = new StringBuilder();
		sb.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;'>");
		sb.append("<tr>")
				.append("<th>ID</th>")
				.append("<th>CE OI</th>")
				.append("<th>CE OI Chg</th>")
				.append("<th>CE OI Int</th>")
				.append("<th>CE Volume</th>")
				.append("<th>CE IV</th>")
				.append("<th>CE IV Chg</th>")
				.append("<th>CE LTP Chg</th>")
				.append("<th>Strike Price</th>")
				.append("<th>PE OI</th>")
				.append("<th>PE OI Chg</th>")
				.append("<th>PE OI Int</th>")
				.append("<th>PE Volume</th>")
				.append("<th>PE IV</th>")
				.append("<th>PE IV Chg</th>")
				.append("<th>PE LTP Chg</th>")
				.append("</tr>");
		for (StrikeSetupEntity strike : strikes) {
			sb.append("<tr>")
					.append("<td>").append(strike.getId()).append("</td>")
					.append("<td>").append(strike.getCeOi()).append("</td>")
					.append("<td>").append(String.format("%.2f", strike.getCeOiChg())).append("</td>")
					.append("<td>").append(strike.getCeOiInt() != null ? strike.getCeOiInt() : "").append("</td>")
					.append("<td>").append(strike.getCeVolume()).append("</td>")
					.append("<td>").append(String.format("%.2f", strike.getCeIv())).append("</td>")
					.append("<td>").append(String.format("%.2f", strike.getCeIvChg())).append("</td>")
					.append("<td>").append(String.format("%.2f", strike.getCeLtpChg())).append("</td>")
					.append("<td>").append(String.format("%.2f", strike.getStrikePrice())).append("</td>")
					.append("<td>").append(strike.getPeOi()).append("</td>")
					.append("<td>").append(String.format("%.2f", strike.getPeOiChg())).append("</td>")
					.append("<td>").append(strike.getPeOiInt() != null ? strike.getPeOiInt() : "").append("</td>")
					.append("<td>").append(strike.getPeVolume()).append("</td>")
					.append("<td>").append(String.format("%.2f", strike.getPeIv())).append("</td>")
					.append("<td>").append(String.format("%.2f", strike.getPeIvChg())).append("</td>")
					.append("<td>").append(String.format("%.2f", strike.getPeLtpChg())).append("</td>")
					.append("</tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}

	public String beautifyResults(List<TradeSetupTO> trades) {
		if (trades == null || trades.isEmpty()) return "No trades found.";
		StringBuilder sb = new StringBuilder();
		sb.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;'>");
		sb.append("<tr>")
				.append("<th>Stock</th>")
				.append("<th>OI</th>")
				.append("<th>LTP</th>")
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
					.append("<td>").append(trade.getOiChgPer() != null ? String.format("%.2f", trade.getOiChgPer()) : "").append("</td>")
					.append("<td>").append(trade.getLtpChgPer() != null ? String.format("%.2f", trade.getLtpChgPer()) : "").append("</td>")
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

	public String beautifyTestResults(List<TradeSetupTO> trades) {
		if (trades == null || trades.isEmpty()) return "No trades found.";
		StringBuilder sb = new StringBuilder();
		sb.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse:collapse;'>");
		sb.append("<tr>")
				.append("<th>Stock</th>")
				.append("<th>OI</th>")
				.append("<th>LTP</th>")
				.append("<th>Status</th>")
				.append("<th>Date</th>")
				.append("<th>Time</th>")
				.append("<th>Entry1</th>")
				.append("<th>Time</th>")
				.append("<th>Entry2</th>")
				.append("<th>Time</th>")
				.append("<th>Target1</th>")
				.append("<th>Time</th>")
				.append("<th>Target2</th>")
				.append("<th>Time</th>")
				.append("<th>Stop Loss</th>")
				.append("<th>Time</th>")
				.append("<th>Notes</th>")
				.append("<th>Strategy</th>")
				.append("<th>Type</th>")
				.append("</tr>");
		for (TradeSetupTO trade : trades) {
			sb.append("<tr>")
					.append("<td>").append(trade.getStockSymbol()).append("</td>")
					.append("<td>").append(trade.getOiChgPer() != null ? String.format("%.2f", trade.getOiChgPer()) : "").append("</td>")
					.append("<td>").append(trade.getLtpChgPer() != null ? String.format("%.2f", trade.getLtpChgPer()) : "").append("</td>")
					.append("<td>").append(trade.getStatus() != null ? trade.getStatus() : "").append("</td>")
					.append("<td>").append(trade.getStockDate()).append("</td>")
					.append("<td>").append(trade.getFetchTime()).append("</td>")
					.append("<td>").append(trade.getEntry1() != null ? String.format("%.2f", trade.getEntry1()) : "").append("</td>")
					.append("<td>").append(trade.getEntry1Time() != null ? trade.getEntry1Time() : "").append("</td>")
					.append("<td>").append(trade.getEntry2() != null ? String.format("%.2f", trade.getEntry2()) : "").append("</td>")
					.append("<td>").append(trade.getEntry2Time() != null ? trade.getEntry2Time() : "").append("</td>")
					.append("<td>").append(trade.getTarget1() != null ? String.format("%.2f", trade.getTarget1()) : "").append("</td>")
					.append("<td>").append(trade.getTarget1Time() != null ? trade.getTarget1Time() : "").append("</td>")
					.append("<td>").append(trade.getTarget2() != null ? String.format("%.2f", trade.getTarget2()) : "").append("</td>")
					.append("<td>").append(trade.getTarget2Time() != null ? trade.getTarget2Time() : "").append("</td>")
					.append("<td>").append(trade.getStopLoss1() != null ? String.format("%.2f", trade.getStopLoss1()) : "").append("</td>")
					.append("<td>").append(trade.getStopLoss1Time() != null ? trade.getStopLoss1Time() : "").append("</td>")
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
