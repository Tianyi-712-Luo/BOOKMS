package com.bookms.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 日期工具（教材实验一 DateUtil 风格 + Math）。
 */
public class DateUtil {

    public static final String DATE_PATTERN      = "yyyy-MM-dd";
    public static final String DATETIME_PATTERN  = "yyyy-MM-dd HH:mm:ss";

    /** Date → 字符串（默认 yyyy-MM-dd HH:mm:ss）。 */
    public static String format(Date date) {
        return format(date, DATETIME_PATTERN);
    }

    public static String format(Date date, String pattern) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    /** 字符串 → Date（解析失败返回 null）。 */
    public static Date parse(String str) {
        return parse(str, DATETIME_PATTERN);
    }

    public static Date parse(String str, String pattern) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.parse(str.trim());
        } catch (ParseException e) {
            System.err.println("[DateUtil] 解析失败: " + str + " (pattern=" + pattern + ")");
            return null;
        }
    }

    /** 借出天数（returnDate - borrowDate，向上取整用 Math.ceil）。 */
    public static long daysBetween(Date borrowDate, Date returnDate) {
        if (borrowDate == null || returnDate == null) return 0;
        long diffMs = returnDate.getTime() - borrowDate.getTime();
        return (long) Math.ceil(TimeUnit.MILLISECONDS.toDays(diffMs));
    }

    /** 当前时间 + N 天（借出 / 归还时限常用）。 */
    public static Date plusDays(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
    }

    public static void main(String[] args) {
        System.out.println("[DateUtil] 当前时间: " + format(new Date()));
        System.out.println("[DateUtil] 当前 +30 天: " + format(plusDays(30)));
        System.out.println("[DateUtil] 解析 2026-09-01 09:30:00: " + format(parse("2026-09-01 09:30:00")));
        System.out.println("[DateUtil] 借出天数(09-01 → 09-10): " + daysBetween(
                parse("2026-09-01 09:30:00"), parse("2026-09-10 09:30:00")));
    }
}