package com.stocks.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DateUtil {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	public static Date getDateFromString(String stringDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			date = sdf.parse(stringDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public static String getDateTimeFromCalendar(Calendar calendar) {
		if (calendar == null) return null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return sdf.format(calendar.getTime());
	}

	public static List<String> getDatesOfTheMonth(int month) {

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd");

		Calendar from = Calendar.getInstance();
		from.set(Calendar.YEAR, 2025);
		from.set(Calendar.MONTH, month);
		from.set(Calendar.DATE, 1);
		from.set(Calendar.HOUR, 9);
		from.set(Calendar.MINUTE, 30);
		from.set(Calendar.SECOND, 0);

		Calendar currentDate = Calendar.getInstance();
		currentDate.set(Calendar.HOUR, 1);
		List<String> dates = new ArrayList<>();
		while (from.toInstant().isBefore(currentDate.toInstant())) {

			if (isWeekDay(from)) {
				dates.add(simpleDateFormat.format(from.getTime()));
			}
			from.set(Calendar.DATE, from.get(Calendar.DATE) + 1);

		}

		return dates;

	}

	public static boolean isWeekDay(Calendar from) {
		if (from.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
				from.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			return false;
		}
		return true;
	}

	public static List<String> getWorkingDaysOfMonth(int year, int month) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month - 1); // Java months are 0-based
		calendar.set(Calendar.DATE, 1);
		List<String> workingDays = new ArrayList<>();
		int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		for (int day = 1; day <= maxDay; day++) {
			calendar.set(Calendar.DATE, day);
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
				workingDays.add(simpleDateFormat.format(calendar.getTime()));
			}
		}
		return workingDays;
	}

	public static Calendar start() {

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 9);
		cal.set(Calendar.MINUTE, 15);
		cal.set(Calendar.SECOND, 0);

		return cal;
	}

	public static Calendar end() {

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 15);
		cal.set(Calendar.MINUTE, 29);
		cal.set(Calendar.SECOND, 59);

		return cal;
	}

	public static long getEpochTimeFromString(String dateString) {
		LocalDateTime ldt = LocalDateTime.parse(dateString, DATE_TIME_FORMATTER);
		return ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
	}

	public static String getDateFromCalendar(Calendar calendar) {
		if (calendar == null) return null;
		LocalDateTime ldt = LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
		return DATE_TIME_FORMATTER.format(ldt);
	}
}
