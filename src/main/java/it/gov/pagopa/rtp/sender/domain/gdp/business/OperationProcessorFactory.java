package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


/**
 * Factory for producing {@link OperationProcessor} instances based on the {@link Operation}
 * specified in a given {@link GdpMessage}.
 *
 * <p>This factory supports a mapping between GDP operations and corresponding processor
 * implementations.</p>
 *
 * @see Operation
 * @see GdpMessage
 * @see OperationProcessor
 * @see CreateOperationProcessor
 */
@Component("operationProcessorFactory")
@Slf4j
public class OperationProcessorFactory {

  private final GdpMapper gdpMapper;
  private final SendRTPServiceImpl sendRTPService;
  private final GdpEventHubProperties gdpEventHubProperties;
  private final RegistryDataService registryDataService;


  /**
   * Constructs a new {@code OperationProcessorFactory} with the required dependencies.
   *
   * @param gdpMapper       the GDP-to-RTP mapper; must not be {@code null}
   * @param sendRTPService  the RTP sending service; must not be {@code null}
   * @param gdpEventHubProperties the configuration properties for GDP Event Hub; must not be {@code null}
   * @param registryDataService the Registry Data Service; must not be {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  public OperationProcessorFactory(
          @NonNull final GdpMapper gdpMapper,
          @NonNull final SendRTPServiceImpl sendRTPService,
          @NonNull final GdpEventHubProperties gdpEventHubProperties,
          @NonNull final RegistryDataService registryDataService) {

    this.gdpMapper = Objects.requireNonNull(gdpMapper);
    this.sendRTPService = Objects.requireNonNull(sendRTPService);
    this.gdpEventHubProperties = Objects.requireNonNull(gdpEventHubProperties);
    this.registryDataService = Objects.requireNonNull(registryDataService);
  }


  /**
   * Retrieves an appropriate {@link OperationProcessor} based on the {@link Operation} of the given {@link GdpMessage}.
   *
   * @param gdpMessage the GDP message from which to extract the operation; must not be {@code null}
   * @return a {@link Mono} emitting the resolved {@link OperationProcessor}
   * @throws NullPointerException if {@code gdpMessage} is {@code null}
   */
  @NonNull
  public Mono<OperationProcessor> getProcessor(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "GdpMessage cannot be null");

    return Mono.just(gdpMessage)
        .doFirst(() -> log.debug("Creating processor instance for operation {}", gdpMessage.operation()))
        .map(this::createProcessorInstance)
        .doOnSuccess(processor -> log.debug("Created processor instance for operation {}", gdpMessage.operation()))
        .doOnError(error -> log.error("Error creating processor instance for operation {}", gdpMessage.operation(), error));
  }


  /**
   * Internal method to instantiate the {@link OperationProcessor} for given {@link Operation} and {@link Status}.
   *
   * @param gdpMessage the GDP message from which to extract the operation; must not be {@code null}
   * @return the corresponding {@link OperationProcessor} instance
   * @throws UnsupportedOperationException if the operation is unsupported
   * @throws NullPointerException if {@code operation} is {@code null}
   */
  @NonNull
  private OperationProcessor createProcessorInstance(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "Gdp Message cannot be null");
    Objects.requireNonNull(gdpMessage.operation(), "Operation cannot be null");

    return switch (gdpMessage.operation()) {
      case CREATE -> new CreateOperationProcessor(this.gdpMapper, this.sendRTPService);
      case UPDATE -> this.createUpdateProcessorInstance(gdpMessage);
      case DELETE -> new DeleteOperationProcessor(this.sendRTPService, this.gdpEventHubProperties);
    };
  }


  /**
   * Creates a processor instance for {@link Operation#UPDATE} based on the {@link Status}.
   *
   * @param gdpMessage the GDP message to evaluate; must not be {@code null}
   * @return the appropriate {@link OperationProcessor} for the UPDATE operation
   * @throws UnsupportedOperationException if the UPDATE operation has an unsupported status
   * @throws NullPointerException if the status is {@code null}
   */
  @NonNull
  private OperationProcessor createUpdateProcessorInstance(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "Gdp Message cannot be null");
    Objects.requireNonNull(gdpMessage.operation(), "Operation cannot be null");
    Objects.requireNonNull(gdpMessage.status(), "Status cannot be null");

    return switch (gdpMessage.status()) {
      case PAID ->
          new UpdatePaidOperationProcessor(
              this.registryDataService, this.sendRTPService, this.gdpEventHubProperties);

      case INVALID, EXPIRED->
          new UpdateInvalidOrExpiredOperationProcessor(
              this.sendRTPService, this.gdpEventHubProperties);
      
      case DRAFT -> new UpdateDraftOperationProcessor(
          this.sendRTPService, this.gdpEventHubProperties);

      case VALID -> new UpdateValidOperationException(
          this.registryDataService, this.sendRTPService, this.gdpEventHubProperties);

      default ->
          throw new UnsupportedOperationException(
              String.format("%s %s", gdpMessage.operation(), gdpMessage.status()));
    };
  }

}

