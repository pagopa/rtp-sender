package it.gov.pagopa.rtp.sender.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class DateUtils {

  private DateUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  private static final DateTimeFormatter MILLIS_FORMATTER =
      new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
          .toFormatter();

  public static String localDateTimeToCustomOffsetFormat(LocalDateTime localDateTime) {
    return Optional.ofNullable(localDateTime)
        .map(ldt -> ldt.atZone(ZoneId.of("Europe/Rome")))
        .map(ldt -> ldt.truncatedTo(ChronoUnit.MILLIS))
        .map(MILLIS_FORMATTER::format)
        .orElseThrow(() ->
            new IllegalArgumentException("Couldn't convert local datetime to offset format"));
  }
}
