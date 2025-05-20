package com.stocks.utils;

import com.stocks.dto.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class FormatUtil {

    private static final Logger log = LoggerFactory.getLogger(FormatUtil.class);

    public static LocalTime getTime(String input, int minutes) {
        return LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm:ss")).plusMinutes(minutes);
    }

    public static LocalTime getTimeHHmm(String input) {
        try {
            return LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
           log.error("Error parsing time: " + input, e);
            return null;
        }
    }

    public static String formatTimeHHmm(LocalTime input) {
        return input.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public static String formatTimeHHmmss(LocalTime input) {
        return input.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public static String formatVolume(long volume) {
        if (volume >= 10000000) {
            return String.format("%.1fC", volume / 10000000.0); // Convert to Crores
        } else if (volume >= 100000) {
            return String.format("%.1fL", volume / 100000.0); // Convert to Lakhs
        } else if (volume >= 1000) {
            return String.format("%.1fK", volume / 1000.0); // Convert to Thousands
        } else {
            return String.valueOf(volume); // Return as is for smaller values
        }
    }

    public static LocalTime addMinutes(LocalTime time, int mins){
        return time.plusMinutes(mins);
    }

    public static String getCurDate(Properties properties) {
        if(properties.getStockDate() != null && !properties.getStockDate().isEmpty()) {
            return properties.getStockDate();
        }
        LocalDate currentDate = LocalDate.now();

        // Define the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        // Format the current date
        return currentDate.format(formatter);

    }

    public static String getYesterdayDate(String stockDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date;
        if (stockDate != null && !stockDate.isEmpty()) {
            date = LocalDate.parse(stockDate, formatter);
        } else {
            date = LocalDate.now();
        }
       return date.minusDays(1).format(formatter);
    }
}
