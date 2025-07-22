package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.errors.ServiceProviderNotFoundException;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProvider;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateInvalidOperationProcessorTest {

    private RegistryDataService registryDataService;
    private SendRTPServiceImpl sendRTPService;
    private UpdateInvalidOperationProcessor processor;

    private final UUID rtpId = UUID.randomUUID();
    private final String originalPspId = "original-psp-id";
    private final String receivedPspId = "different-psp-id";

    private Rtp rtp;

    @BeforeEach
    void setUp() {
        registryDataService = mock(RegistryDataService.class);
        sendRTPService = mock(SendRTPServiceImpl.class);

        GdpEventHubProperties gdpProps = createGdpProps();
        processor = new UpdateInvalidOperationProcessor(registryDataService, sendRTPService, gdpProps);

        rtp = Rtp.builder()
                .resourceID(new ResourceID(rtpId))
                .status(RtpStatus.SENT)
                .serviceProviderDebtor(originalPspId)
                .build();
    }

    @Test
    void givenValidRtpAndDifferentPsp_whenProcessing_thenCancelIsTriggered() {
        GdpMessage message = createGdpMessage("4321");

        when(registryDataService.getServiceProvidersByPspTaxCode())
                .thenReturn(Mono.just(Map.of("4321", createServiceProvider(receivedPspId))));

        when(sendRTPService.doCancelRtp(any()))
                .thenReturn(Mono.just(rtp.withStatus(RtpStatus.CANCELLED)));

        StepVerifier.create(processor.updateRtp(rtp, message))
                .expectNextMatches(result -> result.status() == RtpStatus.CANCELLED)
                .verifyComplete();

        verify(sendRTPService).doCancelRtp(rtp);
    }

    @Test
    void givenValidRtpAndSamePsp_whenProcessing_thenCancelIsNotTriggered() {
        GdpMessage message = createGdpMessage("1234");

        when(registryDataService.getServiceProvidersByPspTaxCode())
                .thenReturn(Mono.just(Map.of("1234", createServiceProvider(originalPspId))));

        StepVerifier.create(processor.updateRtp(rtp, message))
                .verifyComplete();

        verify(sendRTPService, never()).doCancelRtp(any());
    }

    @Test
    void givenUnresolvablePsp_whenProcessing_thenServiceProviderNotFoundIsThrown() {
        GdpMessage message = createGdpMessage("9999");

        when(registryDataService.getServiceProvidersByPspTaxCode())
                .thenReturn(Mono.just(Map.of())); // PSP not found

        StepVerifier.create(processor.updateRtp(rtp, message))
                .expectErrorMatches(error ->
                        error instanceof ServiceProviderNotFoundException &&
                                error.getMessage().contains("9999"))
                .verify();

        verify(sendRTPService, never()).doCancelRtp(any());
    }

    @Test
    void givenErrorDuringCancellation_whenProcessing_thenErrorIsPropagated() {
        GdpMessage message = createGdpMessage("4321");

        when(registryDataService.getServiceProvidersByPspTaxCode())
                .thenReturn(Mono.just(Map.of("4321", createServiceProvider(receivedPspId))));

        when(sendRTPService.doCancelRtp(any()))
                .thenReturn(Mono.error(new RuntimeException("Cancellation failed")));

        StepVerifier.create(processor.updateRtp(rtp, message))
                .expectErrorMatches(e -> e instanceof RuntimeException &&
                        e.getMessage().equals("Cancellation failed"))
                .verify();

        verify(sendRTPService).doCancelRtp(rtp);
    }

    private GdpMessage createGdpMessage(String pspTaxCode) {

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
                .due_date(LocalDate.of(2025,1,1))
                .amount(1000)
                .status(GdpMessage.Status.INVALID)
                .psp_code("PSP123")
                .psp_tax_code(pspTaxCode)
                .build();
    }

    private GdpEventHubProperties createGdpProps() {
        return new GdpEventHubProperties(
                "test",
                "dummy-connection-string",
                new GdpEventHubProperties.Consumer("dispatcher", "test")
        );
    }

    private ServiceProvider createServiceProvider(String id) {
        return new ServiceProvider(id, "Test PSP", "TSP001", "12345678901");
    }


}

