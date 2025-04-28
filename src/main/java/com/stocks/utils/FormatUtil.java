package com.stocks.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class FormatUtil {

    public static String formatTime(String input) {
        return LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm:ss"))
                .format(DateTimeFormatter.ofPattern("HH:mm"));
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

    public static String getCurDate(){
        LocalDate currentDate = LocalDate.now();

        // Define the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        // Format the current date
        return currentDate.format(formatter);

    }
}
