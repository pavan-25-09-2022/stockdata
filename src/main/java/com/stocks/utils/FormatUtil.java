package com.stocks.utils;

import com.stocks.dto.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FormatUtil {

	private static final Logger log = LoggerFactory.getLogger(FormatUtil.class);

	public static LocalTime getTime(String input, int minutes) {
		try {
			return LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm:ss")).plusMinutes(minutes);
		} catch (Exception e) {
			return LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm")).plusMinutes(minutes);
		}
	}

	public static LocalTime getTimeHHmm(String input) {
		try {
			return LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm"));
		} catch (Exception e) {
			log.error("Error parsing time: " + input, e);
			return null;
		}
	}

	public static LocalTime getTimeHHmmss(String input) {
		try {
			return LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm:ss"));
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

	public static LocalTime addMinutes(LocalTime time, int mins) {
		return time.plusMinutes(mins);
	}

	public static String getCurDate(Properties properties) {
		if (properties.getStockDate() != null && !properties.getStockDate().isEmpty()) {
			return properties.getStockDate();
		}
		LocalDate currentDate = LocalDate.now();

		// Define the desired format
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

		// Format the current date
		return currentDate.format(formatter);

	}

	public static String addDays(String stockDate, int days) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate date;
		if (stockDate != null && !stockDate.isEmpty()) {
			date = LocalDate.parse(stockDate, formatter);
		} else {
			date = LocalDate.now();
		}
		return date.plusDays(days).format(formatter);
	}

	public static String getMonthExpiry(String inputDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate date = LocalDate.parse(inputDate, formatter);

		// Find last Thursday of the month
		LocalDate lastDayOfMonth = date.withDayOfMonth(date.lengthOfMonth());
		while (lastDayOfMonth.getDayOfWeek() != java.time.DayOfWeek.THURSDAY) {
			lastDayOfMonth = lastDayOfMonth.minusDays(1);
		}
		DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyMMdd");
		if (!date.isAfter(lastDayOfMonth)) {
			return lastDayOfMonth.format(customFormatter);
		} else {
			// Move to next month and find last Thursday
			LocalDate nextMonth = date.plusMonths(1).withDayOfMonth(1);
			LocalDate lastDayOfNextMonth = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
			while (lastDayOfNextMonth.getDayOfWeek() != java.time.DayOfWeek.THURSDAY) {
				lastDayOfNextMonth = lastDayOfNextMonth.minusDays(1);
			}
			return lastDayOfNextMonth.format(customFormatter);
		}
	}

	public static String getLastTuesdayOfTheMonthMonth(String inputDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate date = LocalDate.parse(inputDate, formatter);

		// Find last day of the month
		LocalDate lastDayOfMonth = date.withDayOfMonth(date.lengthOfMonth());

		// Find the last Tuesday of the month
		while (lastDayOfMonth.getDayOfWeek() != java.time.DayOfWeek.TUESDAY) {
			lastDayOfMonth = lastDayOfMonth.minusDays(1);
		}

		DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyMMdd");
		if (!date.isAfter(lastDayOfMonth)) {
			return lastDayOfMonth.format(customFormatter);
		} else {
			// Move to next month and find last Tuesday
			LocalDate nextMonth = date.plusMonths(1).withDayOfMonth(1);
			LocalDate lastDayOfNextMonth = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
			while (lastDayOfNextMonth.getDayOfWeek() != java.time.DayOfWeek.TUESDAY) {
				lastDayOfNextMonth = lastDayOfNextMonth.minusDays(1);
			}
			return lastDayOfNextMonth.format(customFormatter);
		}
	}
}
