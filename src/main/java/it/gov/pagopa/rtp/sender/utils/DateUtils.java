package it.gov.pagopa.rtp.sender.utils;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

  /**
   * Converts a timestamp expressed in microseconds since epoch to a LocalDate
   * in the "Europe/Rome" timezone. The input is divided by 1000 to convert it to milliseconds,
   * then transformed into a LocalDate, discarding the time component.
   *
   * @param timestamp the timestamp in microseconds (e.g. 1753866547153000)
   * @return the corresponding LocalDate in Europe/Rome timezone, or null if the input is invalid
   */
  public static Optional<LocalDate> convertMillisecondsToLocalDate(Long timestamp) {
    if (timestamp == null) {
      return Optional.empty();
    }

    try {
      long millis = (timestamp > 10000000000000L) ? timestamp / 1000 : timestamp;

      LocalDate result = Instant.ofEpochMilli(millis)
          .atZone(ZoneId.of("Europe/Rome"))
          .toLocalDate();

      log.info("Converted timestamp {} to date {}", timestamp, result);
      return Optional.of(result);

    } catch (DateTimeException | ArithmeticException e) {
      log.error("Error converting timestamp {} to LocalDate. {}", timestamp, e.getMessage());
      return Optional.empty();
    }
  }
}
