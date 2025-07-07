package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;


@Slf4j
public class UpdatePaidOperationProcessor extends UpdateOperationProcessor {

  private static final List<RtpStatus> ACCEPTED_STATUSES = List.of(
      RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED
  );

  public UpdatePaidOperationProcessor(
      @NonNull final RegistryDataService registryDataService,
      @NonNull final SendRTPService sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    super(
        registryDataService, sendRTPService, gdpEventHubProperties, ACCEPTED_STATUSES, Status.PAID);
  }


  @Override
  @NonNull
  protected Mono<Rtp> updateRtp(
      @NonNull final Rtp rtp, @NonNull final GdpMessage gdpMessage) {

    return this.retrieveServiceProviderIdByPspTaxCode(gdpMessage.pspTaxCode())
        .filter(pspBic -> pspBic.equals(rtp.serviceProviderDebtor()))
        .flatMap(pspBic -> this.handleSamePsp(rtp))
        .switchIfEmpty(Mono.fromDirect(this.handleDifferentPsp(rtp)));
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
