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
    void givenValidGdpMessage_whenProcessOperation_thenRtpIsCancelled() {
        final var gdpMessage = GdpMessage.builder()
                .id(123L)
                .status(GdpMessage.Status.VALID)
                .build();

        final var rtp = Rtp.builder()
                .resourceID(new ResourceID(UUID.randomUUID()))
                .status(RtpStatus.CREATED)
                .build();

        when(gdpEventHubProperties.eventDispatcher()).thenReturn("test-dispatcher");
        when(sendRTPService.findRtpByCompositeKey(123L, "test-dispatcher")).thenReturn(Mono.just(rtp));
        when(sendRTPService.cancelRtp(rtp.resourceID())).thenReturn(Mono.just(rtp));

        StepVerifier.create(processor.processOperation(gdpMessage))
                .expectNext(rtp)
                .verifyComplete();

        verify(sendRTPService).findRtpByCompositeKey(123L, "test-dispatcher");
        verify(sendRTPService).cancelRtp(rtp.resourceID());
    }

    @Test
    void givenNonValidGdpMessage_whenProcessOperation_thenDoNothing() {
        final var gdpMessage = GdpMessage.builder()
                .id(123L)
                .status(GdpMessage.Status.INVALID)
                .build();

        StepVerifier.create(processor.processOperation(gdpMessage))
                .verifyComplete();

        verifyNoInteractions(sendRTPService);
    }

    @Test
    void givenErrorDuringRtpLookup_whenProcessOperation_thenPropagateError() {
        final var gdpMessage = GdpMessage.builder()
                .id(123L)
                .status(GdpMessage.Status.VALID)
                .build();

        when(gdpEventHubProperties.eventDispatcher()).thenReturn("test-dispatcher");
        when(sendRTPService.findRtpByCompositeKey(123L, "test-dispatcher"))
                .thenReturn(Mono.error(new RuntimeException("lookup failed")));

        StepVerifier.create(processor.processOperation(gdpMessage))
                .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("lookup failed"))
                .verify();

        verify(sendRTPService).findRtpByCompositeKey(123L, "test-dispatcher");
        verify(sendRTPService, never()).cancelRtp(any());
    }
}