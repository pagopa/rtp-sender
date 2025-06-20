package it.gov.pagopa.rtp.sender.domain.gdp.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OperationProcessorFactoryTest {

  @Mock
  private GdpMapper gdpMapper;

  @Mock
  private SendRTPService sendRTPService;

  private OperationProcessorFactory factory;


  @BeforeEach
  void setUp() {
    factory = new OperationProcessorFactory(gdpMapper, sendRTPService);
  }


  @Test
  void givenNullMessage_whenGetProcessor_thenThrowsException() {
    assertThatThrownBy(() -> factory.getProcessor(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("GdpMessage cannot be null");
  }

  @ParameterizedTest
  @EnumSource(value = Operation.class, names = "CREATE")
  void givenMessageWithSupportedOperation_whenGetProcessor_thenReturnsProcessorInstance(final Operation operation) {
    final var message = GdpMessage.builder()
        .operation(operation)
        .build();

    final var result = factory.getProcessor(message);

    StepVerifier.create(result)
        .assertNext(processor ->
            assertThat(processor).isInstanceOf(CreateOperationProcessor.class))
        .verifyComplete();
  }

  @ParameterizedTest
  @EnumSource(value = Operation.class, names = "CREATE", mode = EnumSource.Mode.EXCLUDE)
  void givenMessageWithUnsupportedOperation_whenGetProcessor_thenThrowsUnsupportedOperationException(final Operation unsupportedOperation) {
    final var message = GdpMessage.builder()
        .operation(unsupportedOperation)
        .build();

    final var result = factory.getProcessor(message);

    StepVerifier.create(result)
        .expectErrorSatisfies(ex -> {
          assertThat(ex).isInstanceOf(UnsupportedOperationException.class);
          assertThat(ex).hasMessage(unsupportedOperation.name());
        })
        .verify();
  }
}
