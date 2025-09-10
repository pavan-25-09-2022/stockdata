package com.stocks.service;

import com.stocks.dto.StockResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);
	@Autowired
	private JavaMailSender javaMailSender;

	@Value("${spring.mail.host}")
	private String mailHost;

	@Value("${spring.mail.port}")
	private int mailPort;

	@Value("${spring.mail.username}")
	private String mailUsername;

	@Value("${spring.mail.password}")
	private String mailPassword;

	@Value("${mail.recipients}")
	private String recipients;


//    public void sendEmail(String to, String subject, String body) {
//        MimeMessageHelper message = new SimpleMailMessage();
//        message.(someHtmlMessage, "text/html; charset=utf-8");
//        message.setTo(to);
//        message.setSubject(subject);
//        message.setText(body);
//        mailSender.send(message);
//    }

	public void sendMail(String subject, String content) {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		try {
			MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
			messageHelper.setFrom(mailUsername);
			String[] sendTo = recipients.split(",");
			messageHelper.setTo(sendTo);
			messageHelper.setSubject(subject);
			messageHelper.setText(content, true);
            javaMailSender.send(mimeMessage);
		} catch (Exception ex) {
			log.error("send mail error", ex);
		}
	}

	public void sendEmailList(List<String> body, String subject) {
		try {
			MimeMessage message = javaMailSender.createMimeMessage();
			message.setFrom(mailUsername);
			message.addRecipients(Message.RecipientType.TO, recipients);

			String mailBody = buildBody(body);
			message.setContent(mailBody, "text/html");
			message.setSubject(subject);
     		javaMailSender.send(message);
		} catch (Exception ex) {
			log.error("send mail error", ex);
		}
	}

	public String buildBody(List<String> list) {

		StringBuilder builder = new StringBuilder();
		for (String string : list) {
			builder.append(string);
			builder.append("<br>");
		}
		return builder.toString();

	}

	public String beautifyResults(List<StockResponse> list, com.stocks.dto.Properties properties) {
		StringBuilder htmlContent = new StringBuilder();
		htmlContent.append("<html><body>");
		htmlContent.append("<h2>Stock Report</h2>");
		double totalPositiveProfit = 0;
		double totalNegativeProfit = 0;
		int negativeStocks = 0;
		int positiveStocks = 0;

		htmlContent.append("<table border='1' style='border-collapse: collapse; width: 100%; table-layout: auto;'>");
		htmlContent.append("<tr>")
				.append("<th>P</th>")
				.append("<th>Stock</th>")
				.append("<th>RSI</th>")
				.append("<th>OC</th>")
				.append("<th>Price</th>")
				.append("<th>Time</th>")
				.append("<th>OI</th>")
				.append("<th>SL</th>")
				.append("<th>EOD OI</th>")
				.append("<th>Vol</th>")
				.append("<th>Init C</th>")
//                .append("<th>Chge in %</th>")
//                .append("<th>YDB</th>")
		;
		if ("test".equalsIgnoreCase(properties.getEnv())) {
			htmlContent.append("<th>profit</th>")
					.append("<th>sellPrice</th>")
					.append("<th>buyPrice</th>")
					.append("<th>buyTime</th>")
					.append("<th>sellTime</th>");
		}
		if ("test1".equalsIgnoreCase(properties.getEnv())) {
			htmlContent
					.append("<th>sellPrice</th>")
					.append("<th>sellTime</th>");
		}
		htmlContent.append("</tr>");

		// Group rows by stock type
		List<StockResponse> nStocks = list.stream()
				.filter(stock -> "N".equals(stock.getStockType()))
				.sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
				.collect(Collectors.toList());

		List<StockResponse> pStocks = list.stream()
				.filter(stock -> "P".equals(stock.getStockType()))
				.sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
				.collect(Collectors.toList());

// Append rows for N stocks
		Map<String, List<StockResponse>> nsStocks = nStocks.stream()
				.collect(Collectors.groupingBy(StockResponse::getOiInterpretation));

		for (Map.Entry<String, List<StockResponse>> entry : nsStocks.entrySet()) {
			List<StockResponse> stocks = entry.getValue();
			for (StockResponse stock : stocks) {
//                if (stock.getPriority() == 0) {
//                    continue;
//                }
				htmlContent.append("<tr style='background-color: #ffcccc;'>")
						.append("<td>").append(stock.getPriority()).append("</td>")
						.append("<td>").append(stock.getStock()).append("</td>")
						.append("<td>").append(stock.getRsi()).append("</td>")
						.append("<td>").append(stock.getStockType()).append("</td>")
						.append("<td>").append(stock.getCurrentPrice()).append("</td>")
						.append("<td>").append(stock.getStartTime()).append("-").append(stock.getEndTime()).append("</td>")
						.append("<td>").append(stock.getOiInterpretation()).append("</td>")
						.append("<td>").append(stock.getStopLoss()).append("</td>")
						.append("<td>").append(stock.getEodData()).append("</td>")
						.append("<td>").append(stock.getCurCandle() != null ? stock.getCurCandle().getVolume() : "").append("</td>")
//                        .append("<td>").append(stock.getCay()).append("</td>")
//                        .append("<td>").append(stock.getYestDayBreak()).append("</td>")
				;
				if (stock.getStockProfitResult() != null) {
					negativeStocks = negativeStocks + 1;
					totalNegativeProfit = totalNegativeProfit + stock.getStockProfitResult().getProfit();
					if ("test".equalsIgnoreCase(properties.getEnv())) {
						htmlContent.append("<td style='background-color: ")
								.append(stock.getStockProfitResult().getProfit() > 0 ? "#ccffcc" : "#ffcccc")
								.append(";'>")
								.append(String.format("%.2f", stock.getStockProfitResult().getProfit()))
								.append("</td>")
								.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getSellPrice())).append("</td>")
								.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getBuyPrice())).append("</td>")
								.append("<td>").append(stock.getStockProfitResult().getBuyTime()).append("</td>")
								.append("<td>").append(stock.getStockProfitResult().getSellTime()).append("</td>");
					} else if ("test1".equalsIgnoreCase(properties.getEnv())) {

					}
					htmlContent.append("</tr>");
				}
			}
			htmlContent.append("<tr style='height: 20px;'></tr>");

			// Add one empty row after each group
		}

		htmlContent.append("<tr style='height: 20px;'></tr>");

// Append rows for P stocks
		Map<String, List<StockResponse>> psStocks = pStocks.stream()
				.collect(Collectors.groupingBy(StockResponse::getOiInterpretation));

		for (Map.Entry<String, List<StockResponse>> entry : psStocks.entrySet()) {
			List<StockResponse> stocks = entry.getValue();
			for (StockResponse stock : stocks) {
//                if (stock.getPriority() == 0) {
//                    continue;
//                }
				htmlContent.append("<tr style='background-color: #ccffcc;'>")
						.append("<td>").append(stock.getPriority()).append("</td>")
						.append("<td>").append(stock.getStock()).append("</td>")
						.append("<td>").append(stock.getRsi()).append("</td>")
						.append("<td>").append(stock.getStockType()).append("</td>")
						.append("<td>").append(stock.getCurrentPrice()).append("</td>")
						.append("<td>").append(stock.getStartTime()).append("-").append(stock.getEndTime()).append("</td>")
						.append("<td>").append(stock.getOiInterpretation()).append("</td>")
						.append("<td>").append(stock.getStopLoss()).append("</td>")
						.append("<td>").append(stock.getEodData()).append("</td>")
						.append("<td>").append(stock.getCurCandle() != null ? stock.getCurCandle().getVolume() : "").append("</td>")
//                        .append("<td>").append(stock.getCay()).append("</td>")
//                        .append("<td>").append(stock.getYestDayBreak()).append("</td>")
				;
				if (stock.getStockProfitResult() != null) {
					positiveStocks = positiveStocks + 1;
					totalPositiveProfit = totalPositiveProfit + stock.getStockProfitResult().getProfit();
					if ("test".equalsIgnoreCase(properties.getEnv())) {

						htmlContent.append("<td style='background-color: ")
								.append(stock.getStockProfitResult().getProfit() > 0 ? "#ccffcc" : "#ffcccc")
								.append(";'>")
								.append(String.format("%.2f", stock.getStockProfitResult().getProfit()))
								.append("</td>")
								.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getSellPrice())).append("</td>")
								.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getBuyPrice())).append("</td>")
								.append("<td>").append(stock.getStockProfitResult().getBuyTime()).append("</td>")
								.append("<td>").append(stock.getStockProfitResult().getSellTime()).append("</td>");
					} else if ("test1".equalsIgnoreCase(properties.getEnv())) {
						htmlContent
								.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getSellPrice())).append("</td>")
								.append("<td>").append(stock.getStockProfitResult().getSellTime()).append("</td>");
					}
				}
				htmlContent.append("</tr>");
			}
			htmlContent.append("<tr style='height: 20px;'></tr>");

			// Add one empty row after each group
		}
		htmlContent.append("</table>");
		htmlContent.append("</body></html>");
		if ("test".equalsIgnoreCase(properties.getEnv())) {
			htmlContent.append("<br>");
			htmlContent.append("<br>");
			htmlContent.append(negativeStocks)
					.append(" negative stocks with profit: ").append(String.format("%.2f", totalNegativeProfit));
			htmlContent.append("<br>");
			htmlContent.append(positiveStocks)
					.append(" positive stocks with profit: ").append(String.format("%.2f", totalPositiveProfit));
		}
		return htmlContent.toString();
	}

	public String beautifyOptChnResults(List<StockResponse> list) {

		StringBuilder htmlContent = new StringBuilder();
		htmlContent.append("<html><body>");
		htmlContent.append("<h2>Stock Report</h2>");
		htmlContent.append("<table border='1' style='border-collapse: collapse; width: 100%; table-layout: auto;'>");
		htmlContent.append("<tr>")
				.append("<th>Stock</th>")
				.append("<th>RSI</th>")
				.append("<th>OC</th>")
				.append("<th>Price</th>")
				.append("<th>Cur St</th>")
				.append("<th>put St</th>")
				.append("<th>cal St</th>")
				.append("<th>Time</th>")
				.append("<th>OI</th>")
				.append("<th>SL</th>")
				.append("<th>Vol</th>")
				.append("<th>Chge in %</th>")
				.append("<th>YDB</th>");
		if (list.get(0).getStockProfitResult() != null) {
			htmlContent.append("<th> profit</th>")
					.append("<th> sellPrice</th>")
					.append("<th> buyPrice</th>")
					.append("<th> buyTime</th>")
					.append("<th> sellTime</th>");
		}
		htmlContent.append("</tr>");

		// Group rows by stock type
		List<StockResponse> nStocks = list.stream()
				.filter(stock -> "N".equals(stock.getStockType()))
				.sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
				.collect(Collectors.toList());

		List<StockResponse> pStocks = list.stream()
				.filter(stock -> "P".equals(stock.getStockType()))
				.sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
				.collect(Collectors.toList());

// Append rows for N stocks
		Map<String, List<StockResponse>> nsStocks = nStocks.stream()
				.collect(Collectors.groupingBy(StockResponse::getOiInterpretation));

		for (Map.Entry<String, List<StockResponse>> entry : nsStocks.entrySet()) {
			List<StockResponse> stocks = entry.getValue();
			for (StockResponse stock : stocks) {
//                if (stock.getPriority() == 0) {
//                    continue;
//                }
				htmlContent.append("<tr style='background-color: #ffcccc;'>")
						.append("<td>").append(stock.getStock()).append("</td>")
						.append("<td>").append(stock.getRsi()).append("</td>")
						.append("<td>").append(stock.getOptionChain()).append("</td>")
						.append("<td>").append(stock.getCurrentPrice()).append("</td>")
						.append("<td>").append(stock.getCurSt()).append("</td>")
						.append("<td>").append(stock.getPutSt()).append("</td>")
						.append("<td>").append(stock.getCallSt()).append("</td>")
						.append("<td>").append(stock.getStartTime()).append("-").append(stock.getEndTime()).append("</td>")
						.append("<td>").append(stock.getOiInterpretation()).append("</td>")
						.append("<td>").append(stock.getStopLoss()).append("</td>")
						.append("<td>").append(stock.getVolume()).append("</td>")
						.append("<td>").append(stock.getCay()).append("</td>")
						.append("<td>").append(stock.getYestDayBreak()).append("</td>");
				if (stock.getStockProfitResult() != null) {
					htmlContent.append("<td style='background-color: ")
							.append(stock.getStockProfitResult().getProfit() > 0 ? "#ccffcc" : "#ffcccc")
							.append(";'>")
							.append(String.format("%.2f", stock.getStockProfitResult().getProfit()))
							.append("</td>")
							.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getSellPrice())).append("</td>")
							.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getBuyPrice())).append("</td>")
							.append("<td>").append(stock.getStockProfitResult().getBuyTime()).append("</td>")
							.append("<td>").append(stock.getStockProfitResult().getSellTime()).append("</td>");
				}
				htmlContent.append("</tr>");
			}
			htmlContent.append("<tr style='height: 20px;'></tr>");

			// Add one empty row after each group
		}

		htmlContent.append("<tr style='height: 20px;'></tr>");

// Append rows for P stocks
		Map<String, List<StockResponse>> psStocks = pStocks.stream()
				.collect(Collectors.groupingBy(StockResponse::getOiInterpretation));

		for (Map.Entry<String, List<StockResponse>> entry : psStocks.entrySet()) {
			List<StockResponse> stocks = entry.getValue();
			for (StockResponse stock : stocks) {
//                if (stock.getPriority() == 0) {
//                    continue;
//                }
				htmlContent.append("<tr style='background-color: #ccffcc;'>")
						.append("<td>").append(stock.getStock()).append("</td>")
						.append("<td>").append(stock.getRsi()).append("</td>")
						.append("<td>").append(stock.getOptionChain()).append("</td>")
						.append("<td>").append(stock.getCurrentPrice()).append("</td>")
						.append("<td>").append(stock.getCurSt()).append("</td>")
						.append("<td>").append(stock.getPutSt()).append("</td>")
						.append("<td>").append(stock.getCallSt()).append("</td>")
						.append("<td>").append(stock.getStartTime()).append("-").append(stock.getEndTime()).append("</td>")
						.append("<td>").append(stock.getOiInterpretation()).append("</td>")
						.append("<td>").append(stock.getStopLoss()).append("</td>")
						.append("<td>").append(stock.getVolume()).append("</td>")
						.append("<td>").append(stock.getCay()).append("</td>")
						.append("<td>").append(stock.getYestDayBreak()).append("</td>");
				if (stock.getStockProfitResult() != null) {
					htmlContent.append("<td style='background-color: ")
							.append(stock.getStockProfitResult().getProfit() > 0 ? "#ccffcc" : "#ffcccc")
							.append(";'>")
							.append(String.format("%.2f", stock.getStockProfitResult().getProfit()))
							.append("</td>")
							.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getSellPrice())).append("</td>")
							.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getBuyPrice())).append("</td>")
							.append("<td>").append(stock.getStockProfitResult().getBuyTime()).append("</td>")
							.append("<td>").append(stock.getStockProfitResult().getSellTime()).append("</td>");
				}
				htmlContent.append("</tr>");
			}
			htmlContent.append("<tr style='height: 20px;'></tr>");

			// Add one empty row after each group
		}
		htmlContent.append("</table>");
		htmlContent.append("</body></html>");
		return htmlContent.toString();
//        StringBuilder htmlContent = new StringBuilder();
//        htmlContent.append("<html><body>");
//        htmlContent.append("<h2>Option chain stock report</h2>");
//        htmlContent.append("<table border='1' style='border-collapse: collapse; width: 100%; table-layout: auto;'>");
//        htmlContent.append("<tr>")
//                .append("<th>Stock</th>")
//                .append("<th>RSI</th>")
//                .append("<th>OI</th>")
//                .append("<th>Cur St</th>")
//                .append("<th>put St</th>")
//                .append("<th>cal St</th>")
//                .append("<th>Price</th>")
//                .append("<th>SL</th>")
//                .append("<th>OC</th>");
//        htmlContent.append("</tr>");
//
//        // Group rows by stock type
//        List<StockResponse> nStocks = list.stream()
//                .filter(stock -> "N".equals(stock.getStockType()))
//                .sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
//                .collect(Collectors.toList());
//
//        List<StockResponse> pStocks = list.stream()
//                .filter(stock -> "P".equals(stock.getStockType()))
//                .sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
//                .collect(Collectors.toList());
//
//// Append rows for N stocks
//
//
//            for (StockResponse stock : nStocks) {
////                if (stock.getPriority() == 0) {
////                    continue;
////                }
//                htmlContent.append("<tr style='background-color: #ffcccc;'>")
//                        .append("<td>").append(stock.getStock()).append("</td>")
//                        .append("<td>").append( stock.getRsi()).append("</td>")
//                        .append("<td>").append( stock.getOiInterpretation()).append("</td>")
//                        .append("<td>").append(stock.getCurSt()).append("</td>")
//                        .append("<td>").append(stock.getPutSt()).append("</td>")
//                        .append("<td>").append(stock.getCallSt()).append("</td>")
//                        .append("<td>").append( stock.getCurrentPrice()).append("</td>")
//                        .append("<td>").append( stock.getStopLoss()).append("</td>")
//                        .append("<td>").append(stock.getOptionChain()).append("</td>");
//                htmlContent.append("</tr>");
//            }
//            htmlContent.append("<tr style='height: 20px;'></tr>");
//            // Add one empty row after each group
//        htmlContent.append("<tr style='height: 20px;'></tr>");
//// Append rows for P stocks
//            for (StockResponse stock : pStocks) {
////                if (stock.getPriority() == 0) {
////                    continue;
////                }
////                 .append("<th>Stock</th>")
////                        .append("<th>RSI</th>")
////                        .append("<th>OI</th>")
////                        .append("<th>Price</th>")
////                        .append("<th>SL</th>")
////                        .append("<th>OC</th>")
//                htmlContent.append("<tr style='background-color: #ccffcc;'>")
//                        .append("<td>").append(stock.getStock()).append("</td>")
//                        .append("<td>").append( stock.getRsi()).append("</td>")
//                        .append("<td>").append( stock.getOiInterpretation()).append("</td>")
//                        .append("<td>").append(stock.getCurSt()).append("</td>")
//                        .append("<td>").append(stock.getPutSt()).append("</td>")
//                        .append("<td>").append(stock.getCallSt()).append("</td>")
//                        .append("<td>").append( stock.getCurrentPrice()).append("</td>")
//                        .append("<td>").append( stock.getStopLoss()).append("</td>")
//                        .append("<td>").append(stock.getOptionChain()).append("</td>")
//                       ;
//                htmlContent.append("</tr>");
//            }
//            htmlContent.append("<tr style='height: 20px;'></tr>");
//
//            // Add one empty row after each group
//
//        htmlContent.append("</table>");
//        htmlContent.append("</body></html>");
//        return htmlContent.toString();
	}

	public String beautifyTrendResults(List<StockResponse> list, com.stocks.dto.Properties properties) {
		StringBuilder htmlContent = new StringBuilder();
		htmlContent.append("<html><body>");
		htmlContent.append("<h2>Stock Trend Report</h2>");
		htmlContent.append("<table border='1' style='border-collapse: collapse; width: 100%; table-layout: auto;'>");
		htmlContent.append("<tr>")
				.append("<th>Stock</th>")
				.append("<th>RSI</th>")
				.append("<th>OC</th>")
				.append("<th>Price</th>")
				.append("<th>Cur St</th>")
				.append("<th>put St</th>")
				.append("<th>cal St</th>")
				.append("<th>Time</th>")
				.append("<th>OI</th>")
				.append("<th>SL</th>")
				.append("<th>Vol</th>")
				.append("<th>Chge in %</th>")
				.append("<th>YDB</th>");
		if (list.get(0).getStockProfitResult() != null) {
			htmlContent.append("<th> profit</th>")
					.append("<th> sellPrice</th>")
					.append("<th> buyPrice</th>")
					.append("<th> buyTime</th>")
					.append("<th> sellTime</th>");
		}
		htmlContent.append("</tr>");

		// Group rows by stock type
		List<StockResponse> nStocks = list.stream()
				.filter(stock -> "N".equals(stock.getStockType()))
				.sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
				.collect(Collectors.toList());

		List<StockResponse> pStocks = list.stream()
				.filter(stock -> "P".equals(stock.getStockType()))
				.sorted(Comparator.comparingInt(StockResponse::getPriority).reversed()) // Order by priority descending
				.collect(Collectors.toList());

// Append rows for N stocks
		Map<String, List<StockResponse>> nsStocks = nStocks.stream()
				.collect(Collectors.groupingBy(StockResponse::getOiInterpretation));

		for (Map.Entry<String, List<StockResponse>> entry : nsStocks.entrySet()) {
			List<StockResponse> stocks = entry.getValue();
			for (StockResponse stock : stocks) {
//                if (stock.getPriority() == 0) {
//                    continue;
//                }
				htmlContent.append("<tr style='background-color: #ffcccc;'>")
						.append("<td>").append(stock.getStock()).append("</td>")
						.append("<td>").append(stock.getRsi()).append("</td>")
						.append("<td>").append(stock.getOptionChain()).append("</td>")
						.append("<td>").append(stock.getCurrentPrice()).append("</td>")
						.append("<td>").append(stock.getCurSt()).append("</td>")
						.append("<td>").append(stock.getPutSt()).append("</td>")
						.append("<td>").append(stock.getCallSt()).append("</td>")
						.append("<td>").append(stock.getStartTime()).append("-").append(stock.getEndTime()).append("</td>")
						.append("<td>").append(stock.getOiInterpretation()).append("</td>")
						.append("<td>").append(stock.getStopLoss()).append("</td>")
						.append("<td>").append(stock.getVolume()).append("</td>")
						.append("<td>").append(stock.getCay()).append("</td>")
						.append("<td>").append(stock.getYestDayBreak()).append("</td>");
				if (stock.getStockProfitResult() != null) {
					htmlContent.append("<td style='background-color: ")
							.append(stock.getStockProfitResult().getProfit() > 0 ? "#ccffcc" : "#ffcccc")
							.append(";'>")
							.append(String.format("%.2f", stock.getStockProfitResult().getProfit()))
							.append("</td>")
							.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getSellPrice())).append("</td>")
							.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getBuyPrice())).append("</td>")
							.append("<td>").append(stock.getStockProfitResult().getBuyTime()).append("</td>")
							.append("<td>").append(stock.getStockProfitResult().getSellTime()).append("</td>");
				}
				htmlContent.append("</tr>");
			}
			htmlContent.append("<tr style='height: 20px;'></tr>");

			// Add one empty row after each group
		}

		htmlContent.append("<tr style='height: 20px;'></tr>");

// Append rows for P stocks
		Map<String, List<StockResponse>> psStocks = pStocks.stream()
				.collect(Collectors.groupingBy(StockResponse::getOiInterpretation));

		for (Map.Entry<String, List<StockResponse>> entry : psStocks.entrySet()) {
			List<StockResponse> stocks = entry.getValue();
			for (StockResponse stock : stocks) {
//                if (stock.getPriority() == 0) {
//                    continue;
//                }
				htmlContent.append("<tr style='background-color: #ccffcc;'>")
						.append("<td>").append(stock.getStock()).append("</td>")
						.append("<td>").append(stock.getRsi()).append("</td>")
						.append("<td>").append(stock.getOptionChain()).append("</td>")
						.append("<td>").append(stock.getCurrentPrice()).append("</td>")
						.append("<td>").append(stock.getCurSt()).append("</td>")
						.append("<td>").append(stock.getPutSt()).append("</td>")
						.append("<td>").append(stock.getCallSt()).append("</td>")
						.append("<td>").append(stock.getStartTime()).append("-").append(stock.getEndTime()).append("</td>")
						.append("<td>").append(stock.getOiInterpretation()).append("</td>")
						.append("<td>").append(stock.getStopLoss()).append("</td>")
						.append("<td>").append(stock.getVolume()).append("</td>")
						.append("<td>").append(stock.getCay()).append("</td>")
						.append("<td>").append(stock.getYestDayBreak()).append("</td>");
				if (stock.getStockProfitResult() != null) {
					htmlContent.append("<td style='background-color: ")
							.append(stock.getStockProfitResult().getProfit() > 0 ? "#ccffcc" : "#ffcccc")
							.append(";'>")
							.append(String.format("%.2f", stock.getStockProfitResult().getProfit()))
							.append("</td>")
							.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getSellPrice())).append("</td>")
							.append("<td>").append(String.format("%.2f", stock.getStockProfitResult().getBuyPrice())).append("</td>")
							.append("<td>").append(stock.getStockProfitResult().getBuyTime()).append("</td>")
							.append("<td>").append(stock.getStockProfitResult().getSellTime()).append("</td>");
				}
				htmlContent.append("</tr>");
			}
			htmlContent.append("<tr style='height: 20px;'></tr>");

			// Add one empty row after each group
		}
		htmlContent.append("</table>");
		htmlContent.append("</body></html>");
		return htmlContent.toString();
	}
}