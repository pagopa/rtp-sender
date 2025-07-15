package it.gov.pagopa.rtp.sender.domain.gdp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
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

  @Mock
  private GdpEventHubProperties gdpEventHubProperties;

  @ParameterizedTest
  @EnumSource(value = Operation.class, names = "CREATE")
  void givenSupportedOperation_whenMessageProcessed_thenRtpIsMappedAndSent(final Operation operation) {
    final var message = GdpMessage.builder()
        .operation(operation)
        .status(GdpMessage.Status.VALID)
        .build();

    final var rtp = Rtp.builder().build();

    when(gdpEventHubProperties.eventDispatcher())
        .thenReturn("test-dispatcher");
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

  @Test
  void givenMessageWithoutOperation_whenProcessed_thenThrowsNullPointerException() {
    final var message = GdpMessage.builder()
        .status(GdpMessage.Status.VALID)
        .build();

    when(gdpEventHubProperties.eventDispatcher())
            .thenReturn("test-dispatcher");
    when(this.operationProcessorFactory.getProcessor(message))
        .thenReturn(Mono.error(new NullPointerException()));

    final var result = gdpMessageProcessor.processMessage(message);

    StepVerifier.create(result)
        .verifyError(NullPointerException.class);

    verify(this.operationProcessorFactory).getProcessor(message);
  }

  @Test
  void givenUnsupportedOperation_whenProcessed_thenThrowsUnsupportedOperationException() {

    // pretend this is an unsupported state
    final var unsupportedOperation = Operation.CREATE;
    final var message = GdpMessage.builder()
            .operation(unsupportedOperation)
            .status(GdpMessage.Status.VALID)
            .build();

    when(gdpEventHubProperties.eventDispatcher())
            .thenReturn("test-dispatcher");
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

  @Test
  void givenValidMessage_whenProcessed_thenContextContainsForeignStatusAndDispatcher() {
    final var message = GdpMessage.builder()
            .operation(Operation.CREATE)
            .status(GdpMessage.Status.VALID)
            .build();

    final var rtp = Rtp.builder().build();

    when(gdpEventHubProperties.eventDispatcher()).thenReturn("test-dispatcher");
    when(operationProcessorFactory.getProcessor(message)).thenReturn(Mono.just(operationProcessor));
    when(operationProcessor.processOperation(message)).thenReturn(
            Mono.deferContextual(ctx -> {
              assertEquals(GdpMessage.Status.VALID, ctx.get("foreignStatus"));
              assertEquals("test-dispatcher", ctx.get("eventDispatcher"));
              return Mono.just(rtp);
            })
    );

    StepVerifier.create(gdpMessageProcessor.processMessage(message))
            .expectNext(rtp)
            .verifyComplete();
  }

  @Test
  void givenMessageWithoutStatus_whenProcessed_thenThrowsNullPointerException() {
    final var message = GdpMessage.builder()
            .operation(GdpMessage.Operation.CREATE)
            .status(null)
            .build();

    StepVerifier.create(gdpMessageProcessor.processMessage(message))
            .expectErrorMatches(error ->
                    error instanceof NullPointerException &&
                            error.getMessage().contains("foreignStatus is required")
            )
            .verify();
  }

  @Test
  void givenNullEventDispatcher_whenProcessed_thenThrowsNullPointerException() {
    final var message = GdpMessage.builder()
            .operation(GdpMessage.Operation.CREATE)
            .status(GdpMessage.Status.VALID)
            .build();

    GdpEventHubProperties props = mock(GdpEventHubProperties.class);
    when(props.eventDispatcher()).thenReturn(null);

    GdpMessageProcessor processor = new GdpMessageProcessor(operationProcessorFactory, props);

    StepVerifier.create(processor.processMessage(message))
            .expectErrorMatches(error ->
                    error instanceof NullPointerException &&
                            error.getMessage().contains("eventDispatcher is required")
            )
            .verify();
  }
}
