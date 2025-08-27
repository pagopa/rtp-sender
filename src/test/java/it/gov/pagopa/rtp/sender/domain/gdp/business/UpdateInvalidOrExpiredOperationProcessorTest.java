package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.errors.RtpNotFoundException;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateInvalidOrExpiredOperationProcessorTest {

    @Mock
    private SendRTPServiceImpl sendRTPService;

    @Mock
    private GdpEventHubProperties gdpProps;

    private UpdateInvalidOrExpiredOperationProcessor processor;

    private final UUID rtpId = UUID.randomUUID();

    private Rtp rtp;

    @BeforeEach
    void setUp() {
    processor =
        new UpdateInvalidOrExpiredOperationProcessor(sendRTPService, gdpProps);

        rtp = Rtp.builder()
                .resourceID(new ResourceID(rtpId))
                .status(RtpStatus.SENT)
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = GdpMessage.Status.class, names = {"INVALID", "EXPIRED"})
    void givenValidRtp_whenProcessing_thenCancelIsTriggered(GdpMessage.Status status) {
        GdpMessage message = createGdpMessage( status);

        when(sendRTPService.cancelRtp(any()))
                .thenReturn(Mono.just(rtp.withStatus(RtpStatus.CANCELLED)));

        StepVerifier.create(processor.updateRtp(rtp, message))
                .expectNextMatches(result -> result.status() == RtpStatus.CANCELLED)
                .verifyComplete();

        verify(sendRTPService).cancelRtp(rtp);
    }

    @ParameterizedTest
    @EnumSource(value = GdpMessage.Status.class, names = {"INVALID", "EXPIRED"})
    void givenErrorDuringCancellation_whenProcessing_thenErrorIsPropagated(GdpMessage.Status status) {
        GdpMessage message = createGdpMessage(status);

        when(sendRTPService.cancelRtp(any()))
                .thenReturn(Mono.error(new RuntimeException("Cancellation failed")));

        StepVerifier.create(processor.updateRtp(rtp, message))
                .expectErrorMatches(e -> e instanceof RuntimeException &&
                        e.getMessage().equals("Cancellation failed"))
                .verify();

        verify(sendRTPService).cancelRtp(rtp);
    }

  @ParameterizedTest
  @EnumSource(value = GdpMessage.Status.class, names = {"INVALID", "EXPIRED"})
  void givenRtpNotFound_whenProcessOperation_thenThrowsRtpNotFoundException(GdpMessage.Status status) {
    final var inputOperationId = 1L;
    final var inputEventDispatcher = "dispatcher";

    final var message = GdpMessage.builder()
        .id(inputOperationId)
        .psp_tax_code("psp-code")
        .status(status)
        .build();

    when(gdpProps.eventDispatcher())
        .thenReturn(inputEventDispatcher);

    when(sendRTPService.findRtpByCompositeKey(inputOperationId, inputEventDispatcher))
        .thenReturn(Mono.error(new RtpNotFoundException(inputOperationId, inputEventDispatcher)));

    final var result = processor.processOperation(message);

    StepVerifier.create(result)
        .expectError(RtpNotFoundException.class)
        .verify();
  }

    private GdpMessage createGdpMessage(GdpMessage.Status status) {

        return GdpMessage
                .builder()
                .id(1L)
                .operation(GdpMessage.Operation.UPDATE)
                .timestamp(System.currentTimeMillis())
                .iuv("IUV123")
                .subject("subject")
                .description("description")
                .ec_tax_code("EC123")
                .debtor_tax_code("DEBT123")
                .nav("NAV123")
                .due_date(1735689600000L)
                .amount(1000)
                .status(status)
                .psp_code("PSP123")
                .psp_tax_code("Fake-psp-tax-code")
                .build();
    }

}

