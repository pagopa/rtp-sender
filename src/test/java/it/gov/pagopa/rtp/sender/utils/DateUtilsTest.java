package it.gov.pagopa.rtp.sender.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void givenValidLocalDateTimeWithTrailingZerosInMillis_whenConvertedToCustomOffsetFormat_thenFormatsCorrectly() {
        // Milliseconds: 100 → should appear as "000"
        LocalDateTime localDateTime000 = LocalDateTime.of(2023, 3, 25, 10, 15, 30, 0);
        String result000 = DateUtils.localDateTimeToCustomOffsetFormat(localDateTime000);
        assertEquals("2023-03-25T10:15:30.000+01:00", result000);

        // Milliseconds: 100 → should appear as ".100"
        LocalDateTime localDateTime = LocalDateTime.of(2023, 3, 27, 10, 15, 30, 100_000_000);
        String result100 = DateUtils.localDateTimeToCustomOffsetFormat(localDateTime);
        assertEquals("2023-03-27T10:15:30.100+02:00", result100);

        // Milliseconds: 110 → should appear as ".110"
        LocalDateTime localDateTime110 = LocalDateTime.of(2023, 3, 25, 10, 15, 30, 110_000_000);
        String result110 = DateUtils.localDateTimeToCustomOffsetFormat(localDateTime110);
        assertEquals("2023-03-25T10:15:30.110+01:00", result110);
    }

    @Test
    void givenLocalDateTimeWithNanos_whenConvertedToCustomOffsetFormat_thenTruncatesToMilliseconds() {
        LocalDateTime localDateTime = LocalDateTime.of(2023, 3, 25, 10, 15, 30, 999_999_999);
        String result = DateUtils.localDateTimeToCustomOffsetFormat(localDateTime);
        assertTrue(result.endsWith("+01:00") || result.endsWith("+02:00"));
        String regex = ".*T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?([+-]\\d{2}:\\d{2})";
        assertTrue(Pattern.matches(regex, result), "Result has more than milliseconds precision: " + result);
    }

    @Test
    void givenNullLocalDateTime_whenConvertedToCustomOffsetFormat_thenThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> DateUtils.localDateTimeToCustomOffsetFormat(null));
        assertEquals("Couldn't convert local datetime to offset format", exception.getMessage());
    }

    @Test
    void givenDateBeforeDSTStart_whenConverted_thenHasStandardOffset() {
        LocalDateTime beforeDST = LocalDateTime.of(2024, 3, 24, 1, 0, 0);
        String result = DateUtils.localDateTimeToCustomOffsetFormat(beforeDST);

        assertTrue(result.endsWith("+01:00"), "Expected +01:00 before DST start");
    }

    @Test
    void givenDateAfterDSTStart_whenConverted_thenHasDaylightOffset() {
        LocalDateTime afterDST = LocalDateTime.of(2024, 3, 31, 3, 0, 0);
        String result = DateUtils.localDateTimeToCustomOffsetFormat(afterDST);

        assertTrue(result.endsWith("+02:00"), "Expected +02:00 after DST start");
    }

    @Test
    void givenDateAfterDSTEnd_whenConverted_thenHasStandardOffset() {
        LocalDateTime afterDSTEnd = LocalDateTime.of(2024, 10, 27, 4, 0, 0);
        String result = DateUtils.localDateTimeToCustomOffsetFormat(afterDSTEnd);

        assertTrue(result.endsWith("+01:00"), "Expected +01:00 after DST ends");
    }
}
