package it.gov.pagopa.rtp.sender.domain.gdp.business;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.errors.RtpNotFoundException;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UpdateDraftOperationProcessorTest {

  private static final Status VALID_STATUS = Status.DRAFT;


  @Mock
  private RegistryDataService registryDataService;

  @Mock
  private SendRTPServiceImpl sendRTPService;

  @Mock
  private GdpEventHubProperties gdpEventHubProperties;

  private UpdateDraftOperationProcessor processor;

  @BeforeEach
  void setUp() {
    lenient().when(gdpEventHubProperties.eventDispatcher()).thenReturn("dispatcher");
    processor = new UpdateDraftOperationProcessor(registryDataService, sendRTPService, gdpEventHubProperties);
  }


  @ParameterizedTest
  @MethodSource("provideValidRtpStatuses")
  void givenValidRtpStatus_whenProcessOperation_thenCancelRtp(RtpStatus rtpStatus) {
    final var inputOperationId = 1L;
    final var inputPspTaxCode = "psp-code";
    final var resourceID = ResourceID.createNew();

    final var message = GdpMessage.builder()
        .id(inputOperationId)
        .psp_tax_code(inputPspTaxCode)
        .status(VALID_STATUS)
        .build();

    final var rtp = Rtp.builder()
        .resourceID(resourceID)
        .status(rtpStatus)
        .serviceProviderDebtor("sp-id")
        .build();

    final var cancelledRtp = Rtp.builder()
        .resourceID(resourceID)
        .status(RtpStatus.CANCELLED)
        .serviceProviderDebtor("sp-id")
        .build();

    when(sendRTPService.findRtpByCompositeKey(inputOperationId, "dispatcher"))
        .thenReturn(Mono.just(rtp));
    when(sendRTPService.cancelRtp(rtp))
        .thenReturn(Mono.just(cancelledRtp));

    final var result = processor.processOperation(message);

    StepVerifier.create(result)
        .expectNext(cancelledRtp)
        .verifyComplete();
  }


  @ParameterizedTest
  @MethodSource("provideValidRtpStatuses")
  void givenVErrorUponCancellingRtp_whenProcessOperation_thenPropagateError(RtpStatus rtpStatus) {
    final var inputOperationId = 1L;
    final var inputPspTaxCode = "psp-code";
    final var resourceID = ResourceID.createNew();

    final var message = GdpMessage.builder()
        .id(inputOperationId)
        .psp_tax_code(inputPspTaxCode)
        .status(VALID_STATUS)
        .build();

    final var rtp = Rtp.builder()
        .resourceID(resourceID)
        .status(rtpStatus)
        .serviceProviderDebtor("sp-id")
        .build();

    final var exception = new RuntimeException("cancel failed");

    when(sendRTPService.findRtpByCompositeKey(inputOperationId, "dispatcher"))
        .thenReturn(Mono.just(rtp));
    when(sendRTPService.cancelRtp(rtp))
        .thenReturn(Mono.error(exception));

    final var result = processor.processOperation(message);

    StepVerifier.create(result)
        .expectErrorMatches(ex ->
            ex instanceof RuntimeException
                && ex.getMessage().equals("cancel failed"))
        .verify();
  }


  private static Stream<Arguments> provideValidRtpStatuses() {
    return Stream.of(
        //Arguments.of(RtpStatus.CREATED),
        //Arguments.of(RtpStatus.SENT),
        //Arguments.of(RtpStatus.ACCEPTED),
        Arguments.of(RtpStatus.USER_ACCEPTED)
    );
  }


  @ParameterizedTest
  @MethodSource("provideInvalidRtpStatuses")
  void givenInvalidRtpStatus_whenProcessOperation_thenThrowsIllegalArgumentException(RtpStatus invalidRtpStatus) {
    final var message = GdpMessage.builder()
        .id(1L)
        .psp_tax_code("psp-code")
        .status(VALID_STATUS)
        .build();

    final var rtp = Rtp.builder()
        .status(invalidRtpStatus)
        .build();

    when(sendRTPService.findRtpByCompositeKey(1L, "dispatcher"))
        .thenReturn(Mono.just(rtp));

    final var result = processor.processOperation(message);

    StepVerifier.create(result)
        .expectError(IllegalArgumentException.class)
        .verify();
  }


  private static Stream<Arguments> provideInvalidRtpStatuses() {
    return Stream.of(
        Arguments.of(RtpStatus.CANCELLED),
        Arguments.of(RtpStatus.REJECTED),
        Arguments.of(RtpStatus.USER_REJECTED),
        Arguments.of(RtpStatus.PAID),
        Arguments.of(RtpStatus.ERROR_SEND),
        Arguments.of(RtpStatus.CANCELLED_PAID),
        Arguments.of(RtpStatus.CANCELLED_ACCR),
        Arguments.of(RtpStatus.CANCELLED_REJECTED),
        Arguments.of(RtpStatus.ERROR_CANCEL)
    );
  }


  @Test
  void givenRtpNotFound_whenProcessOperation_thenThrowsRtpNotFoundException() {
    final var inputOperationId = 1L;
    final var inputEventDispatcher = "dispatcher";

    final var message = GdpMessage.builder()
        .id(inputOperationId)
        .psp_tax_code("psp-code")
        .status(VALID_STATUS)
        .build();

    when(sendRTPService.findRtpByCompositeKey(inputOperationId, inputEventDispatcher))
        .thenReturn(Mono.error(new RtpNotFoundException(inputOperationId, inputEventDispatcher)));

    final var result = processor.processOperation(message);

    StepVerifier.create(result)
        .expectError(RtpNotFoundException.class)
        .verify();
  }


  @ParameterizedTest
  @EnumSource(value = Status.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
  void givenNonDraftStatus_whenProcessOperation_thenThrowsIllegalArgumentException(Status nonPaidStatus) {
    final var message = GdpMessage.builder()
        .id(1L)
        .psp_tax_code("psp-code")
        .status(nonPaidStatus)
        .build();

    final var result = processor.processOperation(message);

    StepVerifier.create(result)
        .expectError(IllegalArgumentException.class)
        .verify();
  }

}