package it.gov.pagopa.rtp.sender.domain.gdp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GdpMessageProcessorTest {

  @Mock
  private GdpMapper gdpMapper;

  @Mock
  private SendRTPService sendRTPService;

  @InjectMocks
  private GdpMessageProcessor gdpMessageProcessor;

  @ParameterizedTest
  @EnumSource(value = Operation.class, names = "CREATE")
  void givenSupportedOperation_whenMessageProcessed_thenRtpIsMappedAndSent(final Operation operation) {
    final var message = GdpMessage.builder()
        .operation(operation)
        .build();

    final var rtp = Rtp.builder().build();

    when(gdpMapper.toRtp(message))
        .thenReturn(rtp);
    when(sendRTPService.send(rtp))
        .thenReturn(Mono.just(rtp));

    final var result = gdpMessageProcessor.processMessage(message);

    StepVerifier.create(result)
        .expectNext(rtp)
        .verifyComplete();

    verify(gdpMapper).toRtp(message);
    verify(sendRTPService).send(rtp);
  }

  @ParameterizedTest
  @EnumSource(value = Operation.class, names = "CREATE", mode = EnumSource.Mode.EXCLUDE)
  void givenUnsupportedOperation_whenMessageProcessed_thenMessageIsSkipped(final Operation unsupportedOperation) {
    final var message = GdpMessage.builder()
        .operation(unsupportedOperation)
        .build();

    final var result = gdpMessageProcessor.processMessage(message);

    StepVerifier.create(result)
        .verifyComplete();

    verifyNoInteractions(gdpMapper, sendRTPService);
  }

  @Test
  void givenCreateOperation_whenMappingReturnsNull_thenProcessingStops() {
    final var message = GdpMessage.builder()
        .operation(Operation.CREATE)
        .build();

    when(gdpMapper.toRtp(message))
        .thenReturn(null);

    final var result = gdpMessageProcessor.processMessage(message);

    StepVerifier.create(result)
        .verifyComplete();

    verify(gdpMapper).toRtp(message);
    verifyNoInteractions(sendRTPService);
  }

  @Test
  void givenNullMessage_whenProcessMessageInvoked_thenThrowsException() {
    assertThrows(NullPointerException.class, () -> gdpMessageProcessor.processMessage(null));
  }
}
