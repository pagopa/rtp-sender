package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;


@Slf4j
public class UpdateDraftOperationProcessor extends UpdateOperationProcessor {

  private static final List<RtpStatus> ACCEPTED_STATUSES = List.of(
      RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED
  );


  protected UpdateDraftOperationProcessor(
      @NonNull final RegistryDataService registryDataService,
      @NonNull final SendRTPServiceImpl sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    super(
        registryDataService, sendRTPService, gdpEventHubProperties, ACCEPTED_STATUSES, Status.DRAFT);
  }


  @Override
  @NonNull
  protected Mono<Rtp> updateRtp(
      @NonNull final Rtp rtp, @NonNull final GdpMessage gdpMessage) {

    return Mono.just(rtp)
        .doFirst(() -> log.info("Cancelling draft RTP with id: {}", rtp.resourceID().getId()))
        .flatMap(this.sendRTPService::cancelRtp)

        .doOnSuccess(cancelledRtp -> log.info("Successfully cancelled draft RTP with id: {}", rtp.resourceID().getId()))
        .doOnError(ex -> log.error("Error cancelling draft RTP: {}", ex.getMessage(), ex));

  }
}
