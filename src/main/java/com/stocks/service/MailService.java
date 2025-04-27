package com.stocks.service;

import com.stocks.dto.StockResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    @Autowired
    private JavaMailSender mailSender;

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

        MimeMessage mimeMessage = mailSender.createMimeMessage();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.starttls.enable", Boolean.TRUE);
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", Boolean.TRUE);
        properties.put("mail.smtp.starttls.required", Boolean.TRUE);
        properties.put("mail.smtp.ssl.enable", Boolean.FALSE);
        properties.put("mail.test-connection", Boolean.TRUE);
        properties.put("mail.debug", Boolean.TRUE);

        mailSender.setJavaMailProperties(properties);

        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            messageHelper.setFrom(mailUsername);
            String[] sendTo = recipients.split(",");
            messageHelper.setTo(sendTo);
            messageHelper.setSubject(subject);
            messageHelper.setText(content, true);
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            log.error("send mail error", ex);
        }
    }

    public String beautifyResults(List<StockResponse> list) {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><body>");
        htmlContent.append("<h2>Stock Report</h2>");
        htmlContent.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        htmlContent.append("<tr>")
                .append("<th>P</th>")
                .append("<th>Stock</th>")
                .append("<th>Cur Price</th>")
                .append("<th>Time</th>")
                .append("<th>OI Interpretation</th>")
                .append("<th>Stop Loss</th>")
                .append("<th>EOD OI</th>");
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
                        .append("<td>").append(stock.getPriority()).append("</td>")
                        .append("<td>").append(stock.getStock()).append("</td>")
                        .append("<td>").append(stock.getCurrentPrice()).append("</td>")
                        .append("<td>").append(stock.getTime()).append("</td>")
                        .append("<td>").append(stock.getOiInterpretation()).append("</td>")
                        .append("<td>").append(stock.getStopLoss()).append("</td>")
                        .append("<td>").append(stock.getEodData()).append("</td>");
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
            // Add one empty row after each group
            htmlContent.append("<tr><td colspan='10'>&nbsp;</td></tr>");
        }


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
                        .append("<td>").append(stock.getCurrentPrice()).append("</td>")
                        .append("<td>").append(stock.getTime()).append("</td>")
                        .append("<td>").append(stock.getOiInterpretation()).append("</td>")
                        .append("<td>").append(stock.getStopLoss()).append("</td>")
                        .append("<td>").append(stock.getEodData()).append("</td>");
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
            // Add one empty row after each group
            htmlContent.append("<tr><td colspan='10'>&nbsp;</td></tr>");
        }
        htmlContent.append("</table>");
        htmlContent.append("</body></html>");
        return htmlContent.toString();
    }
}