package it.gov.pagopa.rtp.sender.service.rtp.handler;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import it.gov.pagopa.rtp.sender.configuration.OpenAPIClientFactory;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig;
import it.gov.pagopa.rtp.sender.configuration.mtlswebclient.WebClientFactory;
import it.gov.pagopa.rtp.sender.epcClient.api.DefaultApi;
import it.gov.pagopa.rtp.sender.service.rtp.SepaRequestToPayMapper;
import reactor.core.publisher.Mono;


/**
 * Handles the process of sending a Request-to-Pay (RTP) request to an external service provider.
 * This class interacts with web clients and API clients to send RTP requests, ensuring secure communication
 * using mutual TLS (mTLS) and OAuth2 authentication when required.
 */
@Component("sendRtpHandler")
@Slf4j
public class SendRtpHandler extends EpcApiInvokerHandler implements RequestHandler<EpcRequest> {

  /**
   * Constructs a {@code SendRtpHandler} with required dependencies.
   *
   * @param webClientFactory        Factory for creating web clients (with or without mTLS).
   * @param epcClientFactory        Factory for creating API clients for EPC (European Payments Council) communication.
   * @param sepaRequestToPayMapper  Mapper for converting RTP requests into EPC-compliant format.
   * @param serviceProviderConfig   Configuration settings for the service provider.
   */
  public SendRtpHandler(
      @NonNull final WebClientFactory webClientFactory,
      @NonNull final OpenAPIClientFactory<DefaultApi> epcClientFactory,
      @NonNull final SepaRequestToPayMapper sepaRequestToPayMapper,
      @NonNull final ServiceProviderConfig serviceProviderConfig) {
    super(webClientFactory, epcClientFactory, sepaRequestToPayMapper, serviceProviderConfig);
  }

  /**
   * Handles an incoming EPC request by sending an RTP request to the external service provider.
   * The request goes through multiple steps, including choosing the appropriate web client (mTLS or simple),
   * setting API credentials, and handling retries in case of failures.
   *
   * @param request The EPC request containing RTP details.
   * @return A {@code Mono} containing the updated EPC request with response data.
   */
  @NonNull
  @Override
  public Mono<EpcRequest> handle(@NonNull final EpcRequest request) {
    return this.createEpcClient(request)
        .doOnNext(epcClient -> log.debug("Successfully created EPC client"))
        .flatMap(epcClient -> {
          final var rtpToSend = request.rtpToSend();
          final var sepaRequest = this.sepaRequestToPayMapper.toEpcRequestToPay(rtpToSend);
          final var basePath = request.serviceProviderFullData().tsp().serviceEndpoint();

          epcClient.getApiClient().setBasePath(basePath);
          this.injectTokenIntoEpcRequest(epcClient, request);

          return Mono.defer(() -> epcClient.postRequestToPayRequests(
                  request.rtpToSend().resourceID().getId(),
                  UUID.randomUUID().toString(),
                  sepaRequest))
              .doFirst(() -> log.info("Sending RTP to {}", rtpToSend.serviceProviderDebtor()))
              .retryWhen(sendRetryPolicy());
        })
        .map(request::withResponse);
  }
}

