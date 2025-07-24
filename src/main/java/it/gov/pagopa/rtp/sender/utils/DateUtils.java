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
import org.springframework.lang.Nullable;

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
   * Converts a timestamp expressed either in milliseconds or microseconds since the epoch
   * to a {@link LocalDate} in the "Europe/Rome" timezone. If the timestamp appears to be
   * in microseconds, it is first converted to milliseconds.
   * <p>
   *
   * @param timestamp the timestamp in milliseconds or microseconds since epoch (e.g. 1753866547153000)
   * @return an {@link Optional} containing the corresponding {@link LocalDate}, or {@link Optional#empty()}
   *         if the input is null or cannot be converted
   */
  public static Optional<LocalDate> convertMillisecondsToLocalDate(@Nullable final Long timestamp) {
    try {
      return Optional.ofNullable(timestamp)
          .map(DateUtils::getMillis)
          .map(Instant::ofEpochMilli)
          .map(instant -> instant.atZone(ZoneId.of("Europe/Rome")))
          .map(zonedDateTime -> {
            final var localDate = zonedDateTime.toLocalDate();
            log.info("Converted timestamp {} to date {}", timestamp, localDate);
            return localDate;
          });

    } catch (DateTimeException | ArithmeticException e) {
      log.error("Error converting timestamp {} to LocalDate. {}", timestamp, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Normalizes a timestamp to milliseconds.
   * <p>
   * If the input value is greater than 13 digits, it is assumed to be in microseconds
   * and is divided by 1000 to convert to milliseconds.
   *
   * @param timestamp the original timestamp in milliseconds or microseconds
   * @return the timestamp normalized to milliseconds
   */
  private static long getMillis(Long timestamp) {
    return (timestamp > 10000000000000L) ? timestamp / 1000 : timestamp;
  }
}
