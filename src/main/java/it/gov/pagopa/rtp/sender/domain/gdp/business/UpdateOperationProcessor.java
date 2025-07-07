package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.errors.ServiceProviderNotFoundException;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProvider;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;


/**
 * Abstract base class for handling {@link Operation#UPDATE} messages with a specific {@link Status}.
 * <p>
 * This class provides the shared logic for retrieving and validating {@link Rtp} instances,
 * ensuring they are in an accepted {@link RtpStatus} and associated with the correct service provider.
 * Subclasses are responsible for implementing the actual update logic via {@link #updateRtp(Rtp, GdpMessage)}.
 * </p>
 *
 * @see OperationProcessor
 * @see GdpMessage
 * @see Rtp
 * @see Status
 * @see RtpStatus
 */
@Slf4j
public abstract class UpdateOperationProcessor implements OperationProcessor {

  protected final RegistryDataService registryDataService;
  protected final SendRTPService sendRTPService;
  protected final GdpEventHubProperties gdpEventHubProperties;
  protected final List<RtpStatus> acceptedStatuses;
  protected final Status statusToHandle;


  /**
   * Constructs a new {@code UpdateOperationProcessor} with required dependencies.
   *
   * @param registryDataService   the service for accessing registry data; must not be {@code null}
   * @param sendRTPService        the service for sending or retrieving RTPs; must not be {@code null}
   * @param gdpEventHubProperties the configuration properties for the Event Hub; must not be {@code null}
   * @param acceptedStatuses      the list of acceptable RTP statuses for processing; must not be {@code null}
   * @param statusToHandle        the specific GDP message status this processor is designed to handle; must not be {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  protected UpdateOperationProcessor(
      @NonNull final RegistryDataService registryDataService,
      @NonNull final SendRTPService sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties,
      @NonNull final List<RtpStatus> acceptedStatuses,
      @NonNull final Status statusToHandle) {

    this.registryDataService = Objects.requireNonNull(registryDataService);
    this.sendRTPService = Objects.requireNonNull(sendRTPService);
    this.gdpEventHubProperties = Objects.requireNonNull(gdpEventHubProperties);
    this.acceptedStatuses = Objects.requireNonNull(acceptedStatuses);
    this.statusToHandle = Objects.requireNonNull(statusToHandle);
  }


  /**
   * Processes given {@link GdpMessage} by validating the message status, retrieving the RTP,
   * checking its eligibility, and then delegating to {@link #updateRtp(Rtp, GdpMessage)}.
   *
   * @param gdpMessage the message to process; must not be {@code null}
   * @return a {@link Mono} emitting the updated {@link Rtp}, or an error if processing fails
   * @throws IllegalArgumentException if the message has an unsupported status or if the RTP is not in an accepted status
   */
  @Override
  @NonNull
  public Mono<Rtp> processOperation(@NonNull final GdpMessage gdpMessage) {
    return Mono.just(gdpMessage)
        .doFirst(() -> log.info("Processing {} message with id {} and status {}", Operation.UPDATE, gdpMessage.id(), gdpMessage.status()))

        .filter(m -> this.statusToHandle.equals(m.status()))
        .switchIfEmpty(
            Mono.error(new IllegalArgumentException("Cannot process message with status " + gdpMessage.status() + " in " + this.statusToHandle + " flow.")))

        .doOnNext(message ->
            log.info("Retrieving RTP with operationId {} and eventDispatcher {}", message.id(), this.gdpEventHubProperties.eventDispatcher()))
        .flatMap(message -> sendRTPService.findRtpByCompositeKey(message.id(), this.gdpEventHubProperties.eventDispatcher()))

        .filter(rtp -> this.acceptedStatuses.contains(rtp.status()))
        .switchIfEmpty(
            Mono.error(new IllegalArgumentException("Cannot update RTP with status " + gdpMessage.status())))

        .flatMap(rtpToUpdate -> this.updateRtp(rtpToUpdate, gdpMessage));
  }


  /**
   * Template method to be implemented by subclasses to define how the RTP should be updated.
   *
   * @param rtp        the RTP to update
   * @param gdpMessage the original GDP message
   * @return a {@link Mono} emitting the updated {@link Rtp}
   */
  protected abstract Mono<Rtp> updateRtp(Rtp rtp, GdpMessage gdpMessage);


  /**
   * Retrieves the service provider ID (PSP BIC) corresponding to the given tax code from the registry.
   *
   * @param pspTaxCode the PSP tax code; must not be {@code null}
   * @return a {@link Mono} emitting the service provider ID
   * @throws ServiceProviderNotFoundException if no service provider is found for the given tax code
   */
  @NonNull
  protected Mono<String> retrieveServiceProviderIdByPspTaxCode(@NonNull final String pspTaxCode) {
    return Mono.just(this.registryDataService)
        .flatMap(RegistryDataService::getServiceProvidersByPspTaxCode)
        .mapNotNull(serviceProviders -> serviceProviders.get(pspTaxCode))
        .map(ServiceProvider::id)
        .switchIfEmpty(Mono.error(new ServiceProviderNotFoundException("No service provider found for tax code " + pspTaxCode)));
  }
}
