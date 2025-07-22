package it.gov.pagopa.rtp.sender.domain.gdp.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OperationProcessorFactoryTest {

  @Mock
  private GdpMapper gdpMapper;

  @Mock
  private SendRTPServiceImpl sendRTPService;

  @Mock
  private GdpEventHubProperties gdpEventHubProperties;

  @Mock
  private RegistryDataService registryDataService;

  @InjectMocks
  private OperationProcessorFactory factory;


  @Test
  void givenNullMessage_whenGetProcessor_thenThrowsException() {
    assertThatThrownBy(() -> factory.getProcessor(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("GdpMessage cannot be null");
  }

  @ParameterizedTest
  @MethodSource("provideSupportedOperationsAndStatuses")
  void givenMessageWithSupportedOperation_whenGetProcessor_thenReturnsProcessorInstance(
      Operation operation, Status status, Class<? extends OperationProcessor> expectedProcessorClass) {
    final var message = GdpMessage.builder()
        .operation(operation)
        .status(status)
        .build();

    final var result = factory.getProcessor(message);

    StepVerifier.create(result)
        .assertNext(processor -> {
          assertThat(processor).isNotNull();
          assertThat(processor.getClass())
              .isEqualTo(expectedProcessorClass);
        })
        .verifyComplete();
  }

  @ParameterizedTest
  @MethodSource("provideUnsupportedOperationsAndStatuses")
  void givenMessageWithUnsupportedOperation_whenGetProcessor_thenThrowsUnsupportedOperationException(Operation unsupportedOperation, Status status) {
    final var message = GdpMessage.builder()
        .operation(unsupportedOperation)
        .status(status)
        .build();

    final var expectedErrorMessage = unsupportedOperation.name() + " " + status.name();

    final var result = factory.getProcessor(message);

    StepVerifier.create(result)
        .expectErrorSatisfies(ex -> {
          assertThat(ex).isInstanceOf(UnsupportedOperationException.class);
          assertThat(ex).hasMessage(expectedErrorMessage);
        })
        .verify();
  }

// ---------- PROVIDERS ----------

  private static Stream<Arguments> provideSupportedOperationsAndStatuses() {
    return Stream.of(
        Arguments.of(Operation.CREATE, Status.VALID, CreateOperationProcessor.class),
        Arguments.of(Operation.CREATE, Status.INVALID, CreateOperationProcessor.class),
        Arguments.of(Operation.CREATE, Status.PARTIALLY_PAID, CreateOperationProcessor.class),
        Arguments.of(Operation.CREATE, Status.PAID, CreateOperationProcessor.class),
        Arguments.of(Operation.CREATE, Status.PUBLISHED, CreateOperationProcessor.class),
        Arguments.of(Operation.CREATE, Status.EXPIRED, CreateOperationProcessor.class),
        Arguments.of(Operation.CREATE, Status.DRAFT, CreateOperationProcessor.class),

        Arguments.of(Operation.DELETE, Status.VALID, DeleteOperationProcessor.class),
        Arguments.of(Operation.DELETE, Status.INVALID, DeleteOperationProcessor.class),
        Arguments.of(Operation.DELETE, Status.PARTIALLY_PAID, DeleteOperationProcessor.class),
        Arguments.of(Operation.DELETE, Status.PAID, DeleteOperationProcessor.class),
        Arguments.of(Operation.DELETE, Status.PUBLISHED, DeleteOperationProcessor.class),
        Arguments.of(Operation.DELETE, Status.EXPIRED, DeleteOperationProcessor.class),
        Arguments.of(Operation.DELETE, Status.DRAFT, DeleteOperationProcessor.class),

        Arguments.of(Operation.UPDATE, Status.PAID, UpdatePaidOperationProcessor.class),
        Arguments.of(Operation.UPDATE, Status.INVALID, UpdateInvalidOrExpiredOperationProcessor.class),
        Arguments.of(Operation.UPDATE, Status.EXPIRED, UpdateInvalidOrExpiredOperationProcessor.class),
        Arguments.of(Operation.UPDATE, Status.DRAFT, UpdateDraftOperationProcessor.class)
    );
  }

  private static Stream<Arguments> provideUnsupportedOperationsAndStatuses() {
    return Stream.of(
        Arguments.of(Operation.UPDATE, Status.VALID),
        Arguments.of(Operation.UPDATE, Status.PARTIALLY_PAID),
        Arguments.of(Operation.UPDATE, Status.PUBLISHED),
        Arguments.of(Operation.UPDATE, Status.DRAFT),
        Arguments.of(Operation.UPDATE, Status.EXPIRED)
    );
  }

}
