package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.errors.RtpNotFoundException;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateValidOperationProcessorTest {

  private static final String SUPPORTED_STATUS_NAME = "VALID";
  private static final GdpMessage.Status SUPPORTED_STATUS = GdpMessage.Status.valueOf(SUPPORTED_STATUS_NAME);

  
  @Mock
  private GdpMapper gdpMapper;

  @Mock
  private SendRTPServiceImpl sendRTPService;

  @Mock
  private GdpEventHubProperties gdpEventHubProperties;

  @InjectMocks
  private UpdateValidOperationProcessor processor;


  @ParameterizedTest
  @MethodSource("provideValidRtpStatuses")
  void givenValidRtpToUpdate_whenProcessRtp_thenThrowsUnsupportedOperationException(RtpStatus rtpStatus) {
    final var inputOperationId = 1L;
    final var inputEventDispatcher = "dispatcher";
    final var resourceID = ResourceID.createNew();

    final var message = GdpMessage.builder()
        .id(inputOperationId)
        .status(SUPPORTED_STATUS)
        .build();

    final var rtp = Rtp.builder()
        .resourceID(resourceID)
        .status(rtpStatus)
        .operationId(inputOperationId)
        .eventDispatcher(inputEventDispatcher)
        .build();

    when(gdpEventHubProperties.eventDispatcher())
        .thenReturn(inputEventDispatcher);
    when(sendRTPService.findRtpByCompositeKey(inputOperationId, inputEventDispatcher))
        .thenReturn(Mono.just(rtp));

    StepVerifier.create(processor.processOperation(message))
        .expectErrorSatisfies(error ->
            Assertions.assertThat(error)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Update VALID existing RTP is not supported yet"))
        .verify();
  }

  @Test
  void givenRtpNotFound_whenProcessOperation_thenCreatesAndSendsRtp() {
    final var inputOperationId = 1L;
    final var inputEventDispatcher = "dispatcher";
    final var resourceID = ResourceID.createNew();

    final var message = GdpMessage.builder()
        .id(inputOperationId)
        .status(SUPPORTED_STATUS)
        .build();

    final var rtp = Rtp.builder()
        .resourceID(resourceID)
        .status(RtpStatus.CREATED)
        .operationId(inputOperationId)
        .eventDispatcher(inputEventDispatcher)
        .build();

    when(gdpEventHubProperties.eventDispatcher())
        .thenReturn(inputEventDispatcher);
    when(sendRTPService.findRtpByCompositeKey(inputOperationId, inputEventDispatcher))
        .thenReturn(Mono.error(new RtpNotFoundException(inputOperationId, inputEventDispatcher)));
    when(gdpMapper.toRtp(message))
        .thenReturn(rtp);
    when(sendRTPService.send(rtp))
        .thenReturn(Mono.just(rtp));

    StepVerifier.create(processor.processOperation(message))
        .expectNext(rtp)
        .verifyComplete();
  }

  @Test
  void givenRtpNotFoundAndGenericException_whenProcessOperation_thenPropagatesException() {
    final var inputOperationId = 1L;
    final var inputEventDispatcher = "dispatcher";

    final var message = GdpMessage.builder()
        .id(inputOperationId)
        .status(SUPPORTED_STATUS)
        .build();

    final var exception = new Exception("Generic exception");

    when(gdpEventHubProperties.eventDispatcher())
        .thenReturn(inputEventDispatcher);
    when(sendRTPService.findRtpByCompositeKey(inputOperationId, inputEventDispatcher))
        .thenReturn(Mono.error(exception));

    StepVerifier.create(processor.processOperation(message))
        .expectErrorMatches(error -> error.equals(exception))
        .verify();
  }

  private static Stream<Arguments> provideValidRtpStatuses() {
    return Stream.of(
        Arguments.of(RtpStatus.CREATED),
        Arguments.of(RtpStatus.SENT),
        Arguments.of(RtpStatus.ACCEPTED),
        Arguments.of(RtpStatus.USER_ACCEPTED)
    );
  }

  @ParameterizedTest
  @EnumSource(value = Status.class, mode = EnumSource.Mode.EXCLUDE, names = SUPPORTED_STATUS_NAME)
  void givenNonValidStatus_whenProcessOperation_thenThrowsIllegalArgumentException(Status nonValidStatus) {
    final var message = GdpMessage.builder()
        .id(1L)
        .psp_tax_code("psp-code")
        .status(nonValidStatus)
        .build();

    final var result = processor.processOperation(message);

    StepVerifier.create(result)
        .expectError(IllegalArgumentException.class)
        .verify();
  }
}
