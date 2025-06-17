package it.gov.pagopa.rtp.sender.domain.gdp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.gdp.business.OperationProcessor;
import it.gov.pagopa.rtp.sender.domain.gdp.business.OperationProcessorFactory;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
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
  private OperationProcessorFactory operationProcessorFactory;

  @InjectMocks
  private GdpMessageProcessor gdpMessageProcessor;

  @Mock
  private OperationProcessor operationProcessor;

  @ParameterizedTest
  @EnumSource(value = Operation.class, names = "CREATE")
  void givenSupportedOperation_whenMessageProcessed_thenRtpIsMappedAndSent(final Operation operation) {
    final var message = GdpMessage.builder()
        .operation(operation)
        .build();

    final var rtp = Rtp.builder().build();

    when(this.operationProcessorFactory.getProcessor(message))
        .thenReturn(Mono.just(this.operationProcessor));
    when(this.operationProcessor.processOperation(message))
        .thenReturn(Mono.just(rtp));

    final var result = gdpMessageProcessor.processMessage(message);

    StepVerifier.create(result)
        .expectNext(rtp)
        .verifyComplete();

    verify(this.operationProcessorFactory).getProcessor(message);
  }

  @ParameterizedTest
  @EnumSource(value = Operation.class, names = "CREATE", mode = EnumSource.Mode.EXCLUDE)
  void givenUnsupportedOperation_whenMessageProcessed_thenMessageIsSkipped(final Operation unsupportedOperation) {
    final var message = GdpMessage.builder()
        .operation(unsupportedOperation)
        .build();

    when(this.operationProcessorFactory.getProcessor(message))
        .thenReturn(Mono.error(new UnsupportedOperationException()));

    final var result = gdpMessageProcessor.processMessage(message);

    StepVerifier.create(result)
        .verifyError(UnsupportedOperationException.class);

    verify(this.operationProcessorFactory).getProcessor(message);
  }

  @Test
  void givenNullMessage_whenProcessMessageInvoked_thenThrowsException() {
    assertThrows(NullPointerException.class, () -> gdpMessageProcessor.processMessage(null));
  }
}
