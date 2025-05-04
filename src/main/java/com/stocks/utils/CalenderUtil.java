package com.stocks.utils;

import java.util.Calendar;
import java.util.Date;

public class CalenderUtil {

    public static  Calendar start() {

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

    public static  Calendar startHoursFromDate(Date date) {

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
}
