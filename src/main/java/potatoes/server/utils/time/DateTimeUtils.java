package potatoes.server.utils.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class DateTimeUtils {
	private static final String KST_ZONE_ID = "Asia/Seoul";

	private static final DateTimeFormatter DATE_TIME_FORMATTER =
		DateTimeFormatter.ofPattern("yyyyMMdd").withZone(TimeZone.getTimeZone(KST_ZONE_ID).toZoneId());

	private static final DateTimeFormatter YEAR_DATE_TIME_FORMATTER =
		DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").withZone(TimeZone.getTimeZone(KST_ZONE_ID).toZoneId());

	public static String getYearMonthDay(Instant instant) {
		return DATE_TIME_FORMATTER.format(instant);
	}

	public static String getYearMonthDayTime(Instant instant) {
		return YEAR_DATE_TIME_FORMATTER.format(instant);
	}

	public static Instant parseYearMonthDayTime(String dateTimeString) {
		LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, YEAR_DATE_TIME_FORMATTER);
		return localDateTime.atZone(TimeZone.getTimeZone(KST_ZONE_ID).toZoneId()).toInstant();
	}

	public static Instant parseYearMonthDay(String dateString) {
		LocalDate localDate = LocalDate.parse(dateString, DATE_TIME_FORMATTER);
		return localDate.atStartOfDay(TimeZone.getTimeZone(KST_ZONE_ID).toZoneId()).toInstant();
	}

	public static Instant parseYearMonthDayOrNull(String dateString) {
		return dateString != null ? parseYearMonthDay(dateString) : null;
	}
}
