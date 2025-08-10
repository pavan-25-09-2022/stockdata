package com.stocks.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

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
}
