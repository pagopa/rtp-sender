package it.gov.pagopa.rtp.sender.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.UUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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

  @Test
  void givenValidInputs_whenGenerateDeterministicIdempotencyKey_thenReturnsDeterministicUUID() {
    String operationSlug = "/operation-slug";
    UUID rtpId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    UUID result1 = IdentifierUtils.generateDeterministicIdempotencyKey(operationSlug, rtpId);
    UUID result2 = IdentifierUtils.generateDeterministicIdempotencyKey(operationSlug, rtpId);

    assertThat(result1).isNotNull();
    assertThat(result1).isEqualTo(result2);
  }

  @Test
  void givenSameOperationSlugAndDifferentRtpIds_whenGenerateDeterministicIdempotencyKey_thenReturnsDifferentUUIDs() {
    String operationSlug = "/operation-slug";
    UUID rtpId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    UUID rtpId2 = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");

    UUID result1 = IdentifierUtils.generateDeterministicIdempotencyKey(operationSlug, rtpId1);
    UUID result2 = IdentifierUtils.generateDeterministicIdempotencyKey(operationSlug, rtpId2);

    assertThat(result1).isNotEqualTo(result2);
  }

  @Test
  void givenDifferentInputs_whenGenerateDeterministicIdempotencyKey_thenReturnsDifferentUUIDs() {
    String operationSlugSend = "/send-operation-slug";
    String operationSlugCancel = "/cancel-operation-slug";
    UUID rtpId = UUID.randomUUID();

    UUID result1 = IdentifierUtils.generateDeterministicIdempotencyKey(operationSlugSend, rtpId);
    UUID result2 = IdentifierUtils.generateDeterministicIdempotencyKey(operationSlugCancel, rtpId);

    assertThat(result1).isNotEqualTo(result2);
  }

  @Test
  void givenNullOperationSlug_whenGenerateDeterministicIdempotencyKey_thenThrowsException() {
    UUID rtpId = UUID.randomUUID();

    assertThatThrownBy(() ->
            IdentifierUtils.generateDeterministicIdempotencyKey(null, rtpId)
    ).isInstanceOf(NullPointerException.class)
            .hasMessage("operationSlug cannot be null");
  }

  @Test
  void givenNullRtpId_whenGenerateDeterministicIdempotencyKey_thenThrowsException() {
    String operationSlug = "/operation-slug";

    assertThatThrownBy(() ->
            IdentifierUtils.generateDeterministicIdempotencyKey(operationSlug, null)
    ).isInstanceOf(NullPointerException.class)
            .hasMessage("rtpId cannot be null");
  }

}

