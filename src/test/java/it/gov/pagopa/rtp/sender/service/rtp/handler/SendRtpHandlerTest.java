package it.gov.pagopa.rtp.sender.service.rtp.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtp.sender.configuration.PagoPaConfigProperties;
import it.gov.pagopa.rtp.sender.domain.rtp.TransactionStatus;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import it.gov.pagopa.rtp.sender.configuration.OpenAPIClientFactory;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig;
import it.gov.pagopa.rtp.sender.configuration.mtlswebclient.WebClientFactory;
import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProviderFullData;
import it.gov.pagopa.rtp.sender.domain.registryfile.TechnicalServiceProvider;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.epcClient.api.DefaultApi;
import it.gov.pagopa.rtp.sender.epcClient.invoker.ApiClient;
import it.gov.pagopa.rtp.sender.epcClient.model.SepaRequestToPayRequestResourceDto;
import it.gov.pagopa.rtp.sender.epcClient.model.SynchronousSepaRequestToPayCreationResponseDto;
import it.gov.pagopa.rtp.sender.service.rtp.SepaRequestToPayMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SendRtpHandlerTest {

  private static final long MAX_ATTEMPTS = 3L;
  private static final long BACKOFF_MIN_DURATION = 100L;
  private static final double BACKOFF_JITTER = 0.5;


  @Mock
  private WebClientFactory webClientFactory;

  @Mock
  private OpenAPIClientFactory<DefaultApi> epcClientFactory;

  @Mock
  private SepaRequestToPayMapper sepaRequestToPayMapper;

  @Mock
  private ServiceProviderConfig serviceProviderConfig;

  @Mock
  private DefaultApi epcClient;

  @Mock
  private ApiClient apiClient;

  @Mock
  private ServiceProviderConfig.Send.Retry retryConfig;

  @Mock
  private PagoPaConfigProperties pagoPaConfigProperties;

  @Mock
  private PagoPaConfigProperties.OperationSlug operationSlug;

  private SendRtpHandler sendRtpHandler;

  @BeforeEach
  void setUp() {
    lenient().when(serviceProviderConfig.send()).thenReturn(mock(ServiceProviderConfig.Send.class));
    lenient().when(serviceProviderConfig.send().retry()).thenReturn(retryConfig);
    lenient().when(retryConfig.maxAttempts()).thenReturn(MAX_ATTEMPTS);
    lenient().when(retryConfig.backoffMinDuration()).thenReturn(BACKOFF_MIN_DURATION);
    lenient().when(retryConfig.backoffJitter()).thenReturn(BACKOFF_JITTER);
    lenient().when(pagoPaConfigProperties.operationSlug()).thenReturn(operationSlug);
    lenient().when(operationSlug.send()).thenReturn("send");

    sendRtpHandler = new SendRtpHandler(webClientFactory, epcClientFactory, sepaRequestToPayMapper, serviceProviderConfig, pagoPaConfigProperties);
  }

  @Test
  void givenValidRequest_whenHandleRtpSend_thenSendRtp() {
    final var transactionStatus = TransactionStatus.ACTC;
    final var resourceId = ResourceID.createNew();
    final var request = mock(EpcRequest.class);
    final var rtpToSend = mock(Rtp.class);
    final var providerData = mock(ServiceProviderFullData.class);
    final var tsp = mock(TechnicalServiceProvider.class);
    final var sepaRequest = mock(SepaRequestToPayRequestResourceDto.class);
    final var sepaResponse = mock(SynchronousSepaRequestToPayCreationResponseDto.class);
    final var webClient = mock(WebClient.class);

    when(rtpToSend.resourceID())
        .thenReturn(resourceId);
    when(request.rtpToSend())
        .thenReturn(rtpToSend);
    when(request.serviceProviderFullData())
        .thenReturn(providerData);
    when(providerData.tsp())
        .thenReturn(tsp);
    when(tsp.serviceEndpoint())
        .thenReturn("https://example.com");
    when(tsp.mtlsEnabled())
        .thenReturn(true);
    when(epcClientFactory.createClient(any()))
        .thenReturn(epcClient);
    when(epcClient.getApiClient())
        .thenReturn(apiClient);
    when(sepaRequestToPayMapper.toEpcRequestToPay(rtpToSend))
        .thenReturn(sepaRequest);
    when(epcClient.postRequestToPayRequests(any(), any(), eq(sepaRequest)))
        .thenReturn(Mono.just(sepaResponse));
    when(request.withResponse(transactionStatus))
        .thenReturn(request);
    when(webClientFactory.createMtlsWebClient())
        .thenReturn(webClient);

    final var result = sendRtpHandler.handle(request);

    StepVerifier.create(result)
        .expectNext(request)
        .verifyComplete();

    verify(epcClient).postRequestToPayRequests(any(), any(), any());
    verify(operationSlug).send();
  }

  @Test
  void givenRequestWithoutCertificate_whenHandleRtpSend_thenUseSimpleWebClient() {
    final var transactionStatus = TransactionStatus.ACTC;
    final var resourceId = ResourceID.createNew();
    final var request = mock(EpcRequest.class);
    final var rtpToSend = mock(Rtp.class);
    final var providerData = mock(ServiceProviderFullData.class);
    final var tsp = mock(TechnicalServiceProvider.class);
    final var sepaRequest = mock(SepaRequestToPayRequestResourceDto.class);
    final var sepaResponse = mock(SynchronousSepaRequestToPayCreationResponseDto.class);
    final var webClient = mock(WebClient.class);

    when(rtpToSend.resourceID())
        .thenReturn(resourceId);
    when(request.rtpToSend())
        .thenReturn(rtpToSend);
    when(request.serviceProviderFullData())
        .thenReturn(providerData);
    when(providerData.tsp())
        .thenReturn(tsp);
    when(tsp.mtlsEnabled())
        .thenReturn(false);
    when(webClientFactory.createSimpleWebClient())
        .thenReturn(webClient);
    when(epcClientFactory.createClient(webClient))
        .thenReturn(epcClient);
    when(epcClient.getApiClient())
        .thenReturn(apiClient);
    when(sepaRequestToPayMapper.toEpcRequestToPay(rtpToSend))
        .thenReturn(sepaRequest);
    when(epcClient.postRequestToPayRequests(any(), any(), eq(sepaRequest)))
        .thenReturn(Mono.just(sepaResponse));
    when(request.withResponse(transactionStatus))
        .thenReturn(request);

    final var result = sendRtpHandler.handle(request);

    StepVerifier.create(result.log())
        .expectNext(request)
        .verifyComplete();

    verify(webClientFactory).createSimpleWebClient();
  }

  @Test
  void givenRequestFails_whenHandleRtpSend_thenRetry() {
    final var resourceId = ResourceID.createNew();
    final var request = mock(EpcRequest.class);
    final var rtpToSend = mock(Rtp.class);
    final var providerData = mock(ServiceProviderFullData.class);
    final var tsp = mock(TechnicalServiceProvider.class);
    final var sepaRequest = mock(SepaRequestToPayRequestResourceDto.class);
    final var webClient = mock(WebClient.class);

    when(rtpToSend.resourceID())
        .thenReturn(resourceId);
    when(request.rtpToSend())
        .thenReturn(rtpToSend);
    when(request.serviceProviderFullData())
        .thenReturn(providerData);
    when(providerData.tsp())
        .thenReturn(tsp);
    when(tsp.serviceEndpoint())
        .thenReturn("https://example.com");
    when(webClientFactory.createSimpleWebClient())
        .thenReturn(webClient);
    when(epcClientFactory.createClient(webClient))
        .thenReturn(epcClient);
    when(epcClient.getApiClient())
        .thenReturn(apiClient);
    when(sepaRequestToPayMapper.toEpcRequestToPay(rtpToSend))
        .thenReturn(sepaRequest);
    when(epcClient.postRequestToPayRequests(any(), any(), any()))
        .thenReturn(Mono.error(new RuntimeException("Simulated Failure")));

    final var result = sendRtpHandler.handle(request);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();

    /*
     * Verify that the client is called MAX_ATTEMPTS times.
     * The +1 is needed to account for the initial deferred call:
     */
    verify(epcClient, times((int) MAX_ATTEMPTS + 1))
        .postRequestToPayRequests(any(), any(), any());
  }


  @Test
  void givenValidRequest_whenSendingFailsOnce_thenRetriesAndSucceeds() {
    final var transactionStatus = TransactionStatus.ACTC;
    final var resourceId = ResourceID.createNew();
    final var request = mock(EpcRequest.class);
    final var rtpToSend = mock(Rtp.class);
    final var providerData = mock(ServiceProviderFullData.class);
    final var tsp = mock(TechnicalServiceProvider.class);
    final var sepaRequest = mock(SepaRequestToPayRequestResourceDto.class);
    final var sepaResponse = mock(SynchronousSepaRequestToPayCreationResponseDto.class);
    final var webClient = mock(WebClient.class);

    when(rtpToSend.resourceID())
        .thenReturn(resourceId);
    when(request.rtpToSend())
        .thenReturn(rtpToSend);
    when(request.serviceProviderFullData())
        .thenReturn(providerData);
    when(providerData.tsp())
        .thenReturn(tsp);
    when(tsp.serviceEndpoint())
        .thenReturn("https://example.com");
    when(webClientFactory.createSimpleWebClient())
        .thenReturn(webClient);
    when(epcClientFactory.createClient(webClient))
        .thenReturn(epcClient);
    when(epcClient.getApiClient())
        .thenReturn(apiClient);
    when(sepaRequestToPayMapper.toEpcRequestToPay(rtpToSend))
        .thenReturn(sepaRequest);

    final var shouldFail = new AtomicBoolean(true);
    when(epcClient.postRequestToPayRequests(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              if (shouldFail.getAndSet(false)) {
                throw new RuntimeException("Simulated call failure");
              }
              return Mono.just(sepaResponse);
            }
        );

    when(request.withResponse(transactionStatus))
        .thenReturn(request);

    final var result = sendRtpHandler.handle(request);

    StepVerifier.create(result)
        .expectNext(request)
        .verifyComplete();
  }

  @Test
  void givenPartiallyFailingRtpSend_whenHandlingRtpSend_thenRequestIdShouldChange() {
    final var transactionStatus = TransactionStatus.ACTC;
    final var numRetries = MAX_ATTEMPTS;
    final var resourceId = ResourceID.createNew();
    final var request = mock(EpcRequest.class);
    final var rtpToSend = mock(Rtp.class);
    final var providerData = mock(ServiceProviderFullData.class);
    final var tsp = mock(TechnicalServiceProvider.class);
    final var sepaRequest = mock(SepaRequestToPayRequestResourceDto.class);
    final var sepaResponse = mock(SynchronousSepaRequestToPayCreationResponseDto.class);
    final var webClient = mock(WebClient.class);

    when(rtpToSend.resourceID())
        .thenReturn(resourceId);
    when(request.rtpToSend())
        .thenReturn(rtpToSend);
    when(request.serviceProviderFullData())
        .thenReturn(providerData);
    when(providerData.tsp())
        .thenReturn(tsp);
    when(tsp.serviceEndpoint())
        .thenReturn("https://example.com");
    when(tsp.mtlsEnabled())
        .thenReturn(true);
    when(epcClientFactory.createClient(any()))
        .thenReturn(epcClient);
    when(epcClient.getApiClient())
        .thenReturn(apiClient);
    when(sepaRequestToPayMapper.toEpcRequestToPay(rtpToSend))
        .thenReturn(sepaRequest);
    when(request.withResponse(transactionStatus))
        .thenReturn(request);
    when(webClientFactory.createMtlsWebClient())
        .thenReturn(webClient);

    final var retryCounter = new AtomicInteger();
    when(epcClient.postRequestToPayRequests(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              if (retryCounter.getAndIncrement() < numRetries - 1) {
                throw new RuntimeException("Simulated call failure");
              }
              return Mono.just(sepaResponse);
            }
        );

    StepVerifier.create(sendRtpHandler.handle(request))
        .expectNext(request)
        .verifyComplete();

    final var requestIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(epcClient, atLeast((int)numRetries))
        .postRequestToPayRequests(any(), requestIdCaptor.capture(), any());

    final var capturedRequestIds = requestIdCaptor.getAllValues();
    assertEquals(numRetries, capturedRequestIds.size());
    assertEquals(numRetries, new HashSet<>(capturedRequestIds).size());
  }

}
