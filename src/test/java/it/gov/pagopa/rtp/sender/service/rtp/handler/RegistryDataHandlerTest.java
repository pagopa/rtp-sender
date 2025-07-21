package it.gov.pagopa.rtp.sender.service.rtp.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.gov.pagopa.rtp.sender.domain.errors.ServiceProviderNotFoundException;
import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProviderFullData;
import it.gov.pagopa.rtp.sender.domain.registryfile.TechnicalServiceProvider;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RegistryDataHandlerTest {

  @Mock
  private RegistryDataService registryDataService;

  private RegistryDataHandler registryDataHandler;

  @BeforeEach
  void setUp() {
    registryDataHandler = new RegistryDataHandler(registryDataService);
  }

  @Test
  void givenValidRequest_whenHandle_thenReturnModifiedRequest() {
    final var spId = "spId";
    final var rtpToSend = mock(Rtp.class);
    final var request = new EpcRequest(rtpToSend, null, null, null);
    final var serviceProviderData = new ServiceProviderFullData(spId, "spName", "psp_tax_code",
        new TechnicalServiceProvider("tspId", "tspName", "tspUrl", "tspSecret", null, true));

    final var expectedRequest = new EpcRequest(rtpToSend, serviceProviderData, null, null);

    when(rtpToSend.serviceProviderDebtor())
        .thenReturn(spId);
    when(registryDataService.getRegistryData()).
        thenReturn(Mono.just(Map.of(spId, serviceProviderData)));

    final var result = registryDataHandler.handle(request);

    StepVerifier.create(result)
        .expectNext(expectedRequest)
        .verifyComplete();
  }

  @Test
  void givenRequestWithoutServiceProvider_whenHandle_thenThrowServiceProviderNotFoundException() {

    final var rtpToSend = mock(Rtp.class);
    final var request = new EpcRequest(rtpToSend, null, null, null);

    when(rtpToSend.serviceProviderDebtor()).thenReturn("unknown-debtor");
    when(registryDataService.getRegistryData()).thenReturn(Mono.just(Collections.emptyMap()));

    final var result = registryDataHandler.handle(request);

    StepVerifier.create(result)
        .expectErrorMatches(throwable -> throwable instanceof ServiceProviderNotFoundException &&
            throwable.getMessage().contains("No service provider found for creditor: unknown-debtor"))
        .verify();
  }

  @Test
  void givenRegistryServiceFails_whenHandle_thenErrorIsPropagated() {

    final var rtpToSend = mock(Rtp.class);
    final var request = new EpcRequest(rtpToSend, null, null, null);
    when(registryDataService.getRegistryData()).thenReturn(Mono.error(new RuntimeException("Service failure")));

    final var result = registryDataHandler.handle(request);

    StepVerifier.create(result)
        .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
            throwable.getMessage().equals("Service failure"))
        .verify();
  }
}
