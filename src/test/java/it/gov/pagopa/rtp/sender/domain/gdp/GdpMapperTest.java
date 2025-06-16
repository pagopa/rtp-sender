package it.gov.pagopa.rtp.sender.domain.gdp;

import static org.assertj.core.api.Assertions.assertThat;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GdpMapperTest {

  private GdpEventHubProperties gdpEventHubProperties;
  private GdpMapper gdpMapper;

  @BeforeEach
  void setUp() {
    this.gdpEventHubProperties = new GdpEventHubProperties(
        "test-name",
        "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test",
        new GdpEventHubProperties.Consumer("test-topic", "test-consumer")
    );

    gdpMapper = new GdpMapper(gdpEventHubProperties);
  }

  @Test
  void givenNullGdpMessage_whenToRtp_thenReturnNull() {
    final var result = gdpMapper.toRtp(null);

    assertThat(result).isNull();
  }

  @Test
  void givenValidGdpMessage_whenToRtp_thenReturnCorrectRtp() {
    final var gdpMessage = GdpMessage.builder()
        .id(1L)
        .operation(GdpMessage.Operation.CREATE)
        .timestamp(1717458660000L)  // 2024-06-04T00:11:00Z
        .iuv("IUV1234567890")
        .subject("Mario Rossi")
        .description("Pagamento TARI")
        .ecTaxCode("12345678901")
        .debtorTaxCode("09876543210")
        .nav("NAV001")
        .dueDate(LocalDate.of(2025, 1, 1))
        .amount(1500)
        .status(GdpMessage.Status.VALID)
        .pspCode("PSPCODE01")
        .pspTaxCode("PSPTAX01")
        .build();

    final var expectedEventDispatcer = this.gdpEventHubProperties.eventDispatcher();

    final var result = gdpMapper.toRtp(gdpMessage);

    assertThat(result).isNotNull();
    assertThat(result.resourceID()).isNotNull();
    assertThat(result.noticeNumber()).isEqualTo("IUV1234567890");
    assertThat(result.amount()).isEqualByComparingTo("1500");
    assertThat(result.description()).isEqualTo("Pagamento TARI");
    assertThat(result.expiryDate()).isEqualTo(LocalDate.of(2025, 1, 1));
    assertThat(result.payerId()).isEqualTo("09876543210");
    assertThat(result.payeeId()).isEqualTo("12345678901");
    assertThat(result.subject()).isEqualTo("Mario Rossi");
    assertThat(result.savingDateTime()).isEqualTo(LocalDateTime.ofInstant(
        Instant.ofEpochMilli(1717458660000L), ZoneOffset.UTC));
    assertThat(result.status()).isEqualTo(RtpStatus.CREATED);
    assertThat(result.events())
        .hasSize(1)
        .allSatisfy(event -> assertThat(event.triggerEvent()).isEqualTo(RtpEvent.CREATE_RTP));
    assertThat(result.operationId()).isEqualTo(1L);
    assertThat(result.eventDispatcher()).isEqualTo(expectedEventDispatcer);
  }
}