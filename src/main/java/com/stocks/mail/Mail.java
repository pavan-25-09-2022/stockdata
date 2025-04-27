package com.stocks.mail;

import com.stocks.dto.ApiResponse;
import com.stocks.dto.StockResponse;
import com.stocks.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Mail {

    @Autowired
    private MailService mailService;

    public void sendMail(String data) {
        // Send email
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = currentDate.format(formatter);
        mailService.sendMail("Stock Report " + formattedDate, data);

    }
    public String beautifyResults(List<StockResponse> list) {
        return mailService.beautifyResults(list);
    }

}
