package it.gov.pagopa.rtp.sender.service.callback;

import com.fasterxml.jackson.databind.JsonNode;
import it.gov.pagopa.rtp.sender.domain.rtp.*;
import it.gov.pagopa.rtp.sender.service.rtp.RtpStatusUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackHandlerTest {

    @Mock
    private RtpRepository rtpRepository;
    @Mock
    private RtpStatusUpdater rtpStatusUpdater;
    @Mock
    private CallbackFieldsExtractor callbackFieldsExtractor;

    @InjectMocks
    private CallbackHandler callbackHandler;

    private final ResourceID resourceID = new ResourceID(UUID.randomUUID());
    private final Rtp rtp = Rtp.builder().resourceID(resourceID).status(RtpStatus.CREATED).build();

    @Test
    void givenValidTransactionStatus_whenHandle_thenAcceptTriggeredAndSaved() {
        final var transactionStatus = TransactionStatus.ACTC;
        final var request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
                .thenReturn(Flux.just(transactionStatus));
        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
                .thenReturn(Mono.just(rtp));
        when(rtpStatusUpdater.triggerAcceptRtp(rtp))
                .thenReturn(Mono.just(rtp));

        StepVerifier.create(callbackHandler.handle(request))
                .expectNext(request)
                .verifyComplete();

        verify(rtpStatusUpdater).triggerAcceptRtp(rtp);
    }

    @Test
    void givenValidRJCTStatus_whenHandle_thenRejectTriggeredAndSaved() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
                .thenReturn(Flux.just(TransactionStatus.RJCT));
        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
                .thenReturn(Mono.just(rtp));
        when(rtpStatusUpdater.triggerRejectRtp(rtp))
                .thenReturn(Mono.just(rtp));

        StepVerifier.create(callbackHandler.handle(request))
                .expectNext(request)
                .verifyComplete();

        verify(rtpStatusUpdater).triggerRejectRtp(rtp);
    }

    @Test
    void givenValidRJCTStatusAndRtpAccepted_whenHandle_thenUserRejectTriggeredAndSaved() {
        final var request = mock(JsonNode.class);
        final var acceptedRtp = this.rtp.withStatus(RtpStatus.ACCEPTED);
        final var userRejectedRtp = this.rtp.withStatus(RtpStatus.USER_REJECTED);

        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
            .thenReturn(Flux.just(TransactionStatus.RJCT));
        when(callbackFieldsExtractor.extractResourceIDSend(request))
            .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
            .thenReturn(Mono.just(acceptedRtp));
        when(rtpStatusUpdater.triggerUserRejectRtp(acceptedRtp))
            .thenReturn(Mono.just(userRejectedRtp));

        StepVerifier.create(callbackHandler.handle(request))
            .expectNext(request)
            .verifyComplete();

        verify(rtpStatusUpdater).triggerUserRejectRtp(acceptedRtp);
    }

    @Test
    void givenValidERRORStatus_whenHandle_thenThrowsIllegalStateException() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
                .thenReturn(Flux.just(TransactionStatus.ERROR));
        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
                .thenReturn(Mono.just(rtp));
        when(rtpStatusUpdater.triggerErrorSendRtp(rtp))
                .thenReturn(Mono.just(rtp));

        StepVerifier.create(callbackHandler.handle(request))
                .expectErrorSatisfies(throwable -> {
                    assert throwable instanceof IllegalStateException;
                    assert throwable.getMessage().contains("TransactionStatus 'ERROR'");
                })
                .verify();

        verify(rtpStatusUpdater).triggerErrorSendRtp(rtp);
    }

    @Test
    void givenInvalidTransactionStatus_whenHandle_thenThrowsIllegalStateException() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
                .thenReturn(Flux.just(TransactionStatus.CNCL));
        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
                .thenReturn(Mono.just(rtp));
        when(rtpStatusUpdater.triggerErrorSendRtp(rtp))
                .thenReturn(Mono.just(rtp));

        StepVerifier.create(callbackHandler.handle(request))
                .expectErrorSatisfies(throwable -> {
                    assert throwable instanceof IllegalStateException;
                    assert throwable.getMessage().contains("Unsupported TransactionStatus 'CNCL'");
                })
                .verify();

        verify(rtpStatusUpdater).triggerErrorSendRtp(rtp);
    }


    @Test
    void givenMissingResourceId_whenHandle_thenIllegalArgumentExceptionThrown() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.error(new IllegalArgumentException("Missing field")));

        StepVerifier.create(callbackHandler.handle(request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void givenTransactionStatusExtractionFails_whenHandle_thenErrorThrown() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
                .thenReturn(Mono.just(rtp));
        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
                .thenReturn(Flux.error(new IllegalArgumentException("Malformed transactionStatus")));

        StepVerifier.create(callbackHandler.handle(request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void givenMultipleTransactionStatuses_whenHandle_thenAllProcessedSequentially() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
                .thenReturn(Flux.just(TransactionStatus.ACTC, TransactionStatus.RJCT));
        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
                .thenReturn(Mono.just(rtp));
        when(rtpStatusUpdater.triggerAcceptRtp(rtp))
                .thenReturn(Mono.just(rtp));
        when(rtpStatusUpdater.triggerRejectRtp(rtp))
                .thenReturn(Mono.just(rtp));

        StepVerifier.create(callbackHandler.handle(request))
                .expectNext(request)
                .verifyComplete();

        verify(rtpStatusUpdater).triggerAcceptRtp(rtp);
        verify(rtpStatusUpdater).triggerRejectRtp(rtp);
    }

    @Test
    void givenNonexistentRtp_whenHandle_thenThrowsIllegalStateException() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
                .thenReturn(Flux.just(TransactionStatus.ACTC));
        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
                .thenReturn(Mono.empty());

        StepVerifier.create(callbackHandler.handle(request))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void givenErrorInTriggerAcceptRtp_whenHandle_thenErrorPropagated() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
                .thenReturn(Flux.just(TransactionStatus.ACTC));
        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
                .thenReturn(Mono.just(rtp));
        when(rtpStatusUpdater.triggerAcceptRtp(rtp))
                .thenReturn(Mono.error(new RuntimeException("Business error")));

        StepVerifier.create(callbackHandler.handle(request))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void givenNoTransactionStatusInPayload_whenHandle_thenCompletesWithoutTransition() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractTransactionStatusSend(request))
                .thenReturn(Flux.empty());
        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.just(resourceID));
        when(rtpRepository.findById(resourceID))
                .thenReturn(Mono.just(rtp));

        StepVerifier.create(callbackHandler.handle(request))
                .expectNext(request)
                .verifyComplete();

        verifyNoInteractions(rtpStatusUpdater);
        verify(rtpRepository, never()).save(any());
    }

    @Test
    void givenExtractorReturnsMonoError_whenHandle_thenErrorPropagated() {
        JsonNode request = mock(JsonNode.class);

        when(callbackFieldsExtractor.extractResourceIDSend(request))
                .thenReturn(Mono.error(new RuntimeException("extractor failed")));

        StepVerifier.create(callbackHandler.handle(request))
                .expectError(RuntimeException.class)
                .verify();
    }

}