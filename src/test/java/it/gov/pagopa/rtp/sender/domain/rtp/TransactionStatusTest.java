package it.gov.pagopa.rtp.sender.domain.rtp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStatusTest {

    @ParameterizedTest
    @ValueSource(strings = {"ACCP", "ACWC", "RJCT"})
    void givenValidStatusString_whenFromString_thenReturnCorrectEnum(String input) {
        TransactionStatus result = TransactionStatus.fromString(input);
        assertEquals(TransactionStatus.valueOf(input), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "INVALID"})
    void givenInvalidStringInput_whenFromString_thenThrowsIllegalArgumentException(String input) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TransactionStatus.fromString(input)
        );
        assertEquals("No matching Enum", exception.getMessage());
    }

    @Test
    void givenNullInput_whenFromString_thenThrowsIllegalArgumentException() {
        String input = null;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TransactionStatus.fromString(input)
        );
        assertEquals("Input text must not be null", exception.getMessage());
    }

}
