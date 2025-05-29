package it.gov.pagopa.rtp.sender.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class IdentifierUtilsTest {

  @Test
  void givenValidUuid_whenFormatUuidWithoutHyphens_thenReturnsLowerCaseWithoutHyphens() {
    UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    String result = IdentifierUtils.formatUuidWithoutHyphens(uuid);

    assertEquals("123e4567e89b12d3a456426614174000", result);
  }

  @Test
  void givenNullUuid_whenFormatUuidWithoutHyphens_thenThrowsNullPointerException() {
    UUID uuid = null;

    assertThrows(NullPointerException.class, () -> IdentifierUtils.formatUuidWithoutHyphens(uuid));
  }

  @Test
  void givenUpperCaseUuid_whenFormatUuidWithoutHyphens_thenReturnsLowerCaseWithoutHyphens() {
    UUID uuid = UUID.fromString("123E4567-E89B-12D3-A456-426614174000");

    String result = IdentifierUtils.formatUuidWithoutHyphens(uuid);

    assertEquals("123e4567e89b12d3a456426614174000", result);
  }

  @Test
  void givenValidFormattedUuid_whenUuidRebuilder_thenRebuildsOriginalUuid() {
    UUID originalUuid = UUID.randomUUID();
    String formatted = IdentifierUtils.formatUuidWithoutHyphens(originalUuid);
    UUID rebuilt = IdentifierUtils.uuidRebuilder(formatted);

    assertEquals(originalUuid, rebuilt);
  }


  @Test
  void givenUuidWithoutHyphens_whenUuidRebuilder_thenReturnsCorrectUuid() {
    String uuidWithoutDashes = "123e4567e89b12d3a456426614174000";
    String expectedFormatted = "123e4567-e89b-12d3-a456-426614174000";
    UUID expectedUuid = UUID.fromString(expectedFormatted);

    UUID result = IdentifierUtils.uuidRebuilder(uuidWithoutDashes);

    assertEquals(expectedUuid, result);
  }

  @Test
  void givenUuidWithInvalidHex_whenUuidRebuilder_thenThrowsIllegalArgumentException() {
    String invalidHex = "123e4567e89b12d3a45642661417400g";

    assertThrows(IllegalArgumentException.class, () -> IdentifierUtils.uuidRebuilder(invalidHex));
  }


  @Test
  void givenUuidWithHyphens_whenUuidRebuilder_thenReturnsSameUuid() {
    String uuidWithDashes = "123e4567-e89b-12d3-a456-426614174000";
    UUID expectedUuid = UUID.fromString(uuidWithDashes);

    UUID result = IdentifierUtils.uuidRebuilder(uuidWithDashes);

    assertEquals(expectedUuid, result);
  }

  @Test
  void givenInvalidUuidString_whenUuidRebuilder_thenThrowsIllegalArgumentException() {
    String invalidUuid = "invalid-uuid-string";

    assertThrows(IllegalArgumentException.class, () -> IdentifierUtils.uuidRebuilder(invalidUuid));
  }

  @Test
  void givenNullString_whenUuidRebuilder_thenThrowsNullPointerException() {
    String nullUuid = null;

    assertThrows(NullPointerException.class, () -> IdentifierUtils.uuidRebuilder(nullUuid));
  }

  @Test
  void givenValidUuidWithoutHyphens_whenIsValidUuidWithoutDashes_thenReturnsTrue() {
    String input = "123e4567e89b12d3a456426614174000";
    assertTrue(IdentifierUtils.isValidUuidWithoutDashes(input));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {
          "123e4567-e89b-12d3-a456-426614174000",
          " ",
          "123e4567e89b12d3a45642661417zzzz"
  })
  void givenInvalidStrings_whenIsValidUuidWithoutDashes_thenReturnsFalse(String input) {
    assertFalse(IdentifierUtils.isValidUuidWithoutDashes(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {
          "123e4567e89b12d3a45642661417400",
          "123e4567e89b12d3a4564266141740000"
  })
  void givenWrongLengthUuidStrings_whenIsValidUuidWithoutDashes_thenReturnsFalse(String input) {
    assertFalse(IdentifierUtils.isValidUuidWithoutDashes(input));
  }
}

