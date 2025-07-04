package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.errors.ServiceProviderNotFoundException;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
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
public class UpdatePaidOperationProcessor implements OperationProcessor {

  private static final List<RtpStatus> ACCEPTED_STATUSES = List.of(
      RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED
  );

  private final RegistryDataService registryDataService;
  private final SendRTPService sendRTPService;
  private final GdpEventHubProperties gdpEventHubProperties;


  public UpdatePaidOperationProcessor(
      @NonNull final RegistryDataService registryDataService,
      @NonNull final SendRTPService sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    this.registryDataService = Objects.requireNonNull(registryDataService);
    this.sendRTPService = Objects.requireNonNull(sendRTPService);
    this.gdpEventHubProperties = Objects.requireNonNull(gdpEventHubProperties);
  }


  @Override
  @NonNull
  public Mono<Rtp> processOperation(@NonNull final GdpMessage gdpMessage) {
    return Mono.just(gdpMessage)
        .doFirst(() -> log.info("Processing UPDATE message with id {}", gdpMessage.id()))
        .flatMap(message -> Mono.just(message)
            .filter(m -> Status.PAID.equals(m.status()))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Cannot process message with status " + gdpMessage.status() + " in PAID flow."))))

        .doOnNext(message ->
            log.info("Retrieving RTP with operationId {} and eventDispatcher {}", message.id(), this.gdpEventHubProperties.eventDispatcher()))
        .flatMap(message -> sendRTPService.findRtpByCompositeKey(message.id(), this.gdpEventHubProperties.eventDispatcher()))

        .flatMap(message -> Mono.just(message)
            .filter(rtp -> ACCEPTED_STATUSES.contains(rtp.status()))
            .switchIfEmpty(
                Mono.error(new IllegalArgumentException("Cannot update RTP with status " + gdpMessage.status()))))

        .flatMap(rtp -> this.retrieveServiceProviderIdByPspTaxCode(gdpMessage.pspTaxCode())
            .filter(pspBic -> pspBic.equals(rtp.serviceProviderDebtor()))
            .flatMap(pspBic -> this.handleSamePsp(rtp))
            .switchIfEmpty(Mono.fromDirect(this.handleDifferentPsp(rtp))));
  }


  @NonNull
  private Mono<String> retrieveServiceProviderIdByPspTaxCode(@NonNull final String pspTaxCode) {
    return Mono.just(this.registryDataService)
        .flatMap(RegistryDataService::getServiceProvidersByPspTaxCode)
        .mapNotNull(serviceProviders -> serviceProviders.get(pspTaxCode))
        .map(ServiceProvider::id)
        .switchIfEmpty(Mono.error(new ServiceProviderNotFoundException("No service provider found for tax code " + pspTaxCode)));
  }


  @NonNull
  private Mono<Rtp> handleSamePsp(@NonNull final Rtp rtp) {
    return Mono.error(new UnsupportedOperationException("Not supported yet"));
  }


  @NonNull
  private Mono<Rtp> handleDifferentPsp(@NonNull final Rtp rtp) {
    return Mono.error(new UnsupportedOperationException("Not supported yet"));
  }
}
