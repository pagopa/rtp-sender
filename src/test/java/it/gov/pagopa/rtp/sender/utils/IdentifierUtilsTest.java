package it.gov.pagopa.rtp.sender.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentifierUtilsTest {

  @Test
  void givenValidUuid_whenUuidFormatter_thenFormattedUuidWithoutHyphens() {
    UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    String result = IdentifierUtils.uuidFormatter(uuid);

    assertEquals("123e4567e89b12d3a456426614174000", result);
  }

  @Test
  void givenNullUuid_whenUuidFormatter_thenThrowIllegalArgumentException() {
    UUID uuid = null;

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> IdentifierUtils.uuidFormatter(uuid));
    assertEquals("uuid cannot be null", thrown.getMessage());
  }

  @Test
  void givenEmptyString_whenUuidFormatter_thenThrowIllegalArgumentException() {
    String emptyString = "";

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
      UUID uuid = UUID.fromString(emptyString);
      IdentifierUtils.uuidFormatter(uuid);
    });

    assertEquals("Invalid UUID string: ", thrown.getMessage());
  }
}
