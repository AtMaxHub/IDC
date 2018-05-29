package com.common.cn.resource.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class ComDateUtil {

	public static String MinuteYear = "yyyyMMddHHmm";
	public static String SecondYear = "yyyyMMddHHmmss";
	public static String SecondYear_Hyphen = "yyyy-MM-dd HH:mm:ss";
	public static String DayYear = "yyyyMMdd";
	public static String DayYear_Hyphen = "yyyy-MM-dd";
	public static String MillYear = "yyyyMMddHHmmssSSS";
	public static String MillYear_Hyphen = "yyyy-MM-dd HH:mm:ss.SSS";

	/**
	 * 
	 * {根据传入时间计算到现在的时间差}
	 * 
	 * @param endTime
	 * @return
	 * @author: WWQ
	 * @throws ParseException
	 */
	public static long timeDiff(String endTime) throws ParseException {
		long beginTime = System.currentTimeMillis();
		return timeDiff(beginTime, endTime);
	}

	public static long timeDiff(String beginTime, String endTime) throws ParseException {
		long beginTimeLong = fmt2Date(beginTime, MinuteYear).getTime();
		long endTimeLong = fmt2Date(endTime, MinuteYear).getTime();
		return timeDiff(beginTimeLong, endTimeLong);
	}

	public static long timeDiff(long beginTimeLong, String endTime) throws ParseException {
		long endTimeLong = fmt2Date(endTime, MinuteYear).getTime();
		return timeDiff(beginTimeLong, endTimeLong);
	}

	public static long timeDiff(long beginTimeLong, long endTimeLong) throws ParseException {
		long timeDiff = (endTimeLong - beginTimeLong) / 60000;
		return timeDiff > 0 ? timeDiff : 0L;
	}

	// 将格式化的时间转换为日期 Date
	public static Date fmt2Date(String data2Fmt, String pattern) throws ParseException {
		DateFormat format = new SimpleDateFormat(pattern);
		return format.parse(data2Fmt);
	}

	public static Date fmt2Date(String data2Fmt) throws ParseException {
		return fmt2Date(data2Fmt, SecondYear_Hyphen);
	}

	// ==获取当前时间字符串
	public static String curTime(String fmtStr) {
		Date now = new Date();
		DateFormat format = new SimpleDateFormat(fmtStr);
		return format.format(now);
	}

	public static String curMill() {
		return curTime(MillYear);
	}

	public static String curMill_H() {
		return curTime(MillYear_Hyphen);
	}

	public static String curSec() {
		return curTime(SecondYear);
	}

	public static String curSec_H() {
		return curTime(SecondYear_Hyphen);
	}

	public static String curMinute() {
		return curTime(MinuteYear);
	}

	public static String curDay_H() {
		return curTime(DayYear_Hyphen);
	}

	// 获取现在是当前月的第几天
	public static int getTodaySNofMonth() {
		Calendar cal = Calendar.getInstance();
		int dayOfMonth = cal.get(GregorianCalendar.DAY_OF_MONTH);
		return dayOfMonth;
	}

	public static String getFmtTime_X(int unitStnd, String fmtStr, int amount) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(unitStnd, amount);
		Date resultDay = calendar.getTime();
		DateFormat format = new SimpleDateFormat(fmtStr);
		return format.format(resultDay);
	}

	public static String getFmtTimexDay(int day) {
		return getFmtTimexDay(DayYear_Hyphen, day);
	}

	public static String getFmtTimexDay(String fmtStr, int amount) {
		return getFmtTime_X(Calendar.DAY_OF_MONTH, fmtStr, amount);
	}

	public static String getFmtTimexMonth(String fmtStr, int amount) {
		return getFmtTime_X(Calendar.MONTH, fmtStr, amount);
	}

	public static String getFmtTimexMinute(String fmtStr, int amount) {
		return getFmtTime_X(Calendar.MINUTE, fmtStr, amount);
	}

	public static long getFmtTimexMinute(String fmtStr, int amount, Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, amount);
		Date result = calendar.getTime();
		DateFormat format = new SimpleDateFormat(fmtStr);
		return Long.parseLong(format.format(result));
	}

	public static long getUnixTime(Date date) {
		if (null == date) {
			return 0;
		}
		return date.getTime() / 1000;
	}

	// ================================专用方法=============================
	/**
	 * 
	 * {为剩余的卡在原来的有效期上，加上 amount 月，格式已经固定为 “ yyyyMMdd ”}
	 * 
	 * @param fmtStr
	 * @param amount
	 * @return
	 * @author: WWQ
	 * @throws ParseException
	 */
	public static String getFmtDate4RechargeCard(String date2Fmt, int amount) throws ParseException {
		String pattern = DayYear;
		Date validDate = fmt2Date(date2Fmt, pattern);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(validDate);// 设置为传入的时间
		calendar.add(Calendar.MONTH, amount);
		Date resultDay = calendar.getTime();
		DateFormat format = new SimpleDateFormat(pattern);
		return format.format(resultDay);
	}

}
