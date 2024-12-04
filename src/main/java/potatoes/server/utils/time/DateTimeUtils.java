package potatoes.server.utils.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.springframework.stereotype.Component;

@Component
public class DateTimeUtils {
	private static final String KST_ZONE_ID = "Asia/Seoul";

	private static final DateTimeFormatter DATE_TIME_FORMATTER =
		DateTimeFormatter.ofPattern("yyyyMMdd").withZone(TimeZone.getTimeZone(KST_ZONE_ID).toZoneId());

	public static String getYearMonthDay(Instant instant) {
		return DATE_TIME_FORMATTER.format(instant);
	}

	public static Instant getStartOfDay(String dateStr) {
		if (dateStr == null) {
			return null;
		}
		LocalDate date = LocalDate.parse(dateStr);
		return date.atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	public static Instant getEndOfDay(String dateStr) {
		if (dateStr == null) {
			return null;
		}
		LocalDate date = LocalDate.parse(dateStr);
		return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
	}
}
