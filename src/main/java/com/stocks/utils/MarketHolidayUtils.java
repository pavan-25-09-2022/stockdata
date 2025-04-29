package com.stocks.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MarketHolidayUtils {

    private static final Set<LocalDate> holidays = new HashSet<>();

    static {
        try {
            // Load holidays from holiday.txt
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/holiday.txt"));
            for (String line : lines) {
                holidays.add(LocalDate.parse(line.trim()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getWorkingDay(String date) {
        LocalDate givenDate = LocalDate.parse(date);

        // Check if the given date is a holiday or weekend
        while (isMarketHoliday(givenDate) || isWeekend(givenDate)) {
            givenDate = givenDate.minusDays(1);
        }

        return givenDate.toString();
    }

    public static boolean isMarketHoliday(LocalDate date) {
        return holidays.contains(date);
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}