package it.gov.pagopa.rtp.sender.service.rtp.handler;

import it.gov.pagopa.rtp.sender.configuration.OpenAPIClientFactory;
import it.gov.pagopa.rtp.sender.configuration.PagoPaConfigProperties;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig;
import it.gov.pagopa.rtp.sender.configuration.mtlswebclient.WebClientFactory;
import it.gov.pagopa.rtp.sender.domain.rtp.TransactionStatus;
import it.gov.pagopa.rtp.sender.epcClient.api.DefaultApi;
import it.gov.pagopa.rtp.sender.service.rtp.SepaRequestToPayMapper;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import it.gov.pagopa.rtp.sender.utils.IdentifierUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;


/**
 * Handles the process of sending a Request-to-Pay (RTP) request to an external service provider.
 * This class interacts with web clients and API clients to send RTP requests, ensuring secure communication
 * using mutual TLS (mTLS) and OAuth2 authentication when required.
 */
@Component("sendRtpHandler")
@Slf4j
public class SendRtpHandler extends EpcApiInvokerHandler implements RequestHandler<EpcRequest> {

    private final PagoPaConfigProperties pagoPaConfigProperties;
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
      @NonNull final ServiceProviderConfig serviceProviderConfig,
      @NonNull final PagoPaConfigProperties pagoPaConfigProperties) {
    super(webClientFactory, epcClientFactory, sepaRequestToPayMapper, serviceProviderConfig);
    this.pagoPaConfigProperties = Objects.requireNonNull(pagoPaConfigProperties);
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
          final var idempotencyKey = IdentifierUtils.generateDeterministicIdempotencyKey(
                  this.pagoPaConfigProperties.operationSlug().send(),
                  request.rtpToSend().resourceID().getId()
          );

          epcClient.getApiClient().setBasePath(basePath);
          this.injectTokenIntoEpcRequest(epcClient, request);

          return Mono.defer(() -> epcClient.postRequestToPayRequests(
                  idempotencyKey,
                  UUID.randomUUID().toString(),
                  sepaRequest))
              .doFirst(() -> log.info("Sending RTP to {}", rtpToSend.serviceProviderDebtor()))
              .doOnError(error -> {
                log.error("Error occurred while sending RTP: {}", error);
              })
              .retryWhen(sendRetryPolicy());
        })
        .map(resp -> request.withResponse(TransactionStatus.ACTC))
        .doOnNext(resp -> log.info("Mapping sent RTP to {}", TransactionStatus.ACTC))
        .switchIfEmpty(Mono.just(request))
        .onErrorResume(IllegalStateException.class, ex -> this.handleRetryError(ex, request))
        .doOnNext(resp -> log.info("Response: {}", resp.response()));
  }


  /**
   * Handles the error that occurs when retrying the RTP request.
   * If the error is a {@link WebClientResponseException} with a {@code HttpStatus.BAD_REQUEST} status code, the method
   * updates the {@link EpcRequest} with a {@code TransactionStatus.RJCT} (Rejected) status. Otherwise, it updates
   * the request with an {@code TransactionStatus.ERROR} status.
   *
   * @param ex      The {@link IllegalStateException} that occurred during the retry
   * @param request The {@link EpcRequest} that was being processed
   * @return A {@code Mono<EpcRequest>} containing the updated {@code request}
   * @throws NullPointerException if {@code ex} or {@code request} is {@code null}
   */
  @NonNull
  private Mono<EpcRequest> handleRetryError(
      @NonNull final IllegalStateException ex,
      @NonNull final EpcRequest request) {

    log.warn("Handling error upon sending RTP to {}", request.serviceProviderFullData().tsp().serviceEndpoint());

    final var statusCodeOptional = Optional.of(ex)
        .filter(Exceptions::isRetryExhausted)
        .map(Throwable::getCause)
        .filter(WebClientResponseException.class::isInstance)
        .map(WebClientResponseException.class::cast)
        .map(WebClientResponseException::getStatusCode);

    statusCodeOptional
            .map(HttpStatusCode::value)
            .map(String::valueOf)
            .ifPresent(httpStatusCode -> MDC.put("status_code", httpStatusCode));

    return statusCodeOptional.filter(httpStatusCode -> httpStatusCode.isSameCodeAs(HttpStatus.BAD_REQUEST))
        .map(statusCode -> Mono.just(request.withResponse(TransactionStatus.RJCT)))
        .orElse(Mono.just(request.withResponse(TransactionStatus.ERROR)));
  }
}

