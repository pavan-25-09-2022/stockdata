package com.stocks.utils;

import java.util.Calendar;
import java.util.Date;

public class CalendarUtil {

	public static Calendar start() {

		Calendar cal = Calendar.getInstance();
		//cal.set(Calendar.DAY_OF_MONTH, 30);
		//cal.set(Calendar.MONTH, 9);
		cal.set(Calendar.HOUR_OF_DAY, 9);
		cal.set(Calendar.MINUTE, 15);
		cal.set(Calendar.SECOND, 0);

		return cal;
	}

	public static Calendar end() {

		Calendar cal = Calendar.getInstance();
		// cal.set(Calendar.DAY_OF_MONTH, 30);
		//cal.set(Calendar.MONTH, 9);
		cal.set(Calendar.HOUR_OF_DAY, 15);
		cal.set(Calendar.MINUTE, 29);
		cal.set(Calendar.SECOND, 59);

		return cal;
	}

	public static Calendar startHoursFromDate(Date date) {

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 9);
		cal.set(Calendar.MINUTE, 15);
		cal.set(Calendar.SECOND, 0);

		return cal;
	}

	public static Calendar endHoursFromDate(Date date) {

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 15);
		cal.set(Calendar.MINUTE, 29);
		cal.set(Calendar.SECOND, 59);

		return cal;
	}

	/**
	 * Builds a Calendar object from a date and time string, adding a specified number of days.
	 *
	 * @param date The date in "yyyy-MM-dd" format.
	 * @param time The time in "HH:mm" format.
	 * @return A Calendar object set to the specified date and time, with the added days.
	 */
	public static Calendar buildCalendar(String date, String time) {
		String[] splitTime = time.split(":");
		String[] splitDate = date.split("-");

		Calendar from = Calendar.getInstance();
		from.set(Calendar.YEAR, Integer.parseInt(splitDate[0]));
		from.set(Calendar.MONTH, Integer.parseInt(splitDate[1]) - 1); // Month is 0-based
		from.set(Calendar.DAY_OF_MONTH, Integer.parseInt(splitDate[2]));
		from.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTime[0]));
		from.set(Calendar.MINUTE, Integer.parseInt(splitTime[1]));
		from.set(Calendar.SECOND, 0);
		from.set(Calendar.MILLISECOND, 0);
		return from;
	}
}
