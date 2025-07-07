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


@Slf4j
public abstract class UpdateOperationProcessor implements OperationProcessor {

  protected final RegistryDataService registryDataService;
  protected final SendRTPService sendRTPService;
  protected final GdpEventHubProperties gdpEventHubProperties;
  protected final List<RtpStatus> acceptedStatuses;
  protected final Status statusToHandle;


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


  @Override
  @NonNull
  public Mono<Rtp> processOperation(@NonNull final GdpMessage gdpMessage) {
    return Mono.just(gdpMessage)
        .doFirst(() -> log.info("Processing {} message with id {} and status {}", Operation.UPDATE, gdpMessage.id(), gdpMessage.status()))
        .flatMap(message -> Mono.just(message)
            .filter(m -> this.statusToHandle.equals(m.status()))
            .switchIfEmpty(
                Mono.error(new IllegalArgumentException("Cannot process message with status " + gdpMessage.status() + " in " + this.statusToHandle + " flow."))))

        .doOnNext(message ->
            log.info("Retrieving RTP with operationId {} and eventDispatcher {}", message.id(), this.gdpEventHubProperties.eventDispatcher()))
        .flatMap(message -> sendRTPService.findRtpByCompositeKey(message.id(), this.gdpEventHubProperties.eventDispatcher()))

        .flatMap(message -> Mono.just(message)
            .filter(rtp -> this.acceptedStatuses.contains(rtp.status()))
            .switchIfEmpty(
                Mono.error(new IllegalArgumentException("Cannot update RTP with status " + gdpMessage.status()))))

        .flatMap(rtpToUpdate -> this.updateRtp(rtpToUpdate, gdpMessage));
  }


  protected abstract Mono<Rtp> updateRtp(Rtp rtp, GdpMessage gdpMessage);


  @NonNull
  protected Mono<String> retrieveServiceProviderIdByPspTaxCode(@NonNull final String pspTaxCode) {
    return Mono.just(this.registryDataService)
        .flatMap(RegistryDataService::getServiceProvidersByPspTaxCode)
        .mapNotNull(serviceProviders -> serviceProviders.get(pspTaxCode))
        .map(ServiceProvider::id)
        .switchIfEmpty(Mono.error(new ServiceProviderNotFoundException("No service provider found for tax code " + pspTaxCode)));
  }
}
