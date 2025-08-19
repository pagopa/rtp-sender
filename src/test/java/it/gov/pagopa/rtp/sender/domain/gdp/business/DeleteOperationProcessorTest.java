package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteOperationProcessorTest {

    @Mock
    private SendRTPService sendRTPService;

    @Mock
    private GdpEventHubProperties gdpEventHubProperties;

    private DeleteOperationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DeleteOperationProcessor(sendRTPService, gdpEventHubProperties);
    }

    @Test
    void givenGdpMessage_whenProcessOperation_thenRtpIsCancelled() {
        final var operationId = 123L;
        final var eventDispatcher = "test-dispatcher";
        final var gdpMessage = GdpMessage.builder()
                .id(operationId)
                .status(null)
                .build();

        final var rtp = Rtp.builder()
                .resourceID(new ResourceID(UUID.randomUUID()))
                .status(RtpStatus.CREATED)
                .build();

        when(gdpEventHubProperties.eventDispatcher()).thenReturn(eventDispatcher);
        when(sendRTPService.findRtpByCompositeKey(operationId, eventDispatcher)).thenReturn(Mono.just(rtp));
        when(sendRTPService.cancelRtp(rtp)).thenReturn(Mono.just(rtp));

        StepVerifier.create(processor.processOperation(gdpMessage))
                .expectNext(rtp)
                .verifyComplete();

        verify(sendRTPService).findRtpByCompositeKey(operationId, eventDispatcher);
        verify(sendRTPService).cancelRtp(rtp);
    }

    @Test
    void givenErrorDuringRtpLookup_whenProcessOperation_thenPropagateError() {
        final var operationId = 123L;
        final var eventDispatcher = "test-dispatcher";
        final var gdpMessage = GdpMessage.builder()
                .id(operationId)
                .status(null)
                .build();

        when(gdpEventHubProperties.eventDispatcher()).thenReturn(eventDispatcher);
        when(sendRTPService.findRtpByCompositeKey(operationId, eventDispatcher))
                .thenReturn(Mono.error(new RuntimeException("lookup failed")));

        StepVerifier.create(processor.processOperation(gdpMessage))
                .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("lookup failed"))
                .verify();

        verify(sendRTPService).findRtpByCompositeKey(operationId, eventDispatcher);
        verify(sendRTPService, never()).cancelRtpById(any());
    }
}