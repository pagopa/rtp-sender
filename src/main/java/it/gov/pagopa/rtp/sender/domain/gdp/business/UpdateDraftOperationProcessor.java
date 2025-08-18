package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;


/**
 * Operation processor responsible for handling GDP messages that update RTPs in {@link Status#DRAFT} state.
 */
@Slf4j
public class UpdateDraftOperationProcessor extends UpdateOperationProcessor {

  private static final List<RtpStatus> ACCEPTED_STATUSES = List.of(
      RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED
  );


  /**
   * Constructs a new {@code UpdateOperationProcessor} with required dependencies.
   *
   * @param registryDataService   the service for accessing registry data; must not be {@code null}
   * @param sendRTPService        the service for sending or retrieving RTPs; must not be
   *                              {@code null}
   * @param gdpEventHubProperties the configuration properties for the Event Hub; must not be
   *                              {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  protected UpdateDraftOperationProcessor(
      @NonNull final RegistryDataService registryDataService,
      @NonNull final SendRTPServiceImpl sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    super(
        registryDataService, sendRTPService, gdpEventHubProperties,
        ACCEPTED_STATUSES, Collections.singletonList(Status.DRAFT));
  }


  /**
   * Cancels the RTP if the current state and message are compatible with {@link Status#DRAFT}.
   *
   * @param rtp        the RTP to cancel; must not be {@code null}
   * @param gdpMessage the GDP message triggering the update; must not be {@code null}
   * @return a {@link Mono} emitting the cancelled {@link Rtp}, or an error if the operation fails
   */
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
