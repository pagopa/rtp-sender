package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * {@link OperationProcessor} implementation for handling {@link GdpMessage.Operation#UPDATE} messages
 * with status {@link GdpMessage.Status#INVALID} or {@link GdpMessage.Status#EXPIRED}.
 * <p>
 * If the PSP specified in the message differs from the debtor's service provider in the RTP,
 * the RTP is cancelled. Otherwise, the message is ignored without taking any action.
 * </p>
 *
 * @see UpdateOperationProcessor
 * @see GdpMessage
 * @see Rtp
 */
@Slf4j
public class UpdateInvalidOrExpiredOperationProcessor extends UpdateOperationProcessor {

  private static final List<RtpStatus> VALID_STATUSES =
      List.of(RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED);

  private static final List<GdpMessage.Status> SUPPORTED_STATUSES =
      List.of(GdpMessage.Status.INVALID, GdpMessage.Status.EXPIRED);

  /**
  * Constructs the processor with required dependencies.
  *
  * @param registryDataService     service for retrieving service provider data from the registry
  * @param sendRTPService          service responsible for sending or cancelling RTPs
  * @param gdpEventHubProperties   configuration properties for GDP event hub
  */
  public UpdateInvalidOrExpiredOperationProcessor(
      @NonNull final RegistryDataService registryDataService,
      @NonNull final SendRTPServiceImpl sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    super(registryDataService, sendRTPService, gdpEventHubProperties, VALID_STATUSES, SUPPORTED_STATUSES);
  }

  /**
   * Processes a GDP message with status {@code INVALID} or {@code EXPIRED}.
   *
   * <p>If the PSP in the message does not match the debtor's service provider in the RTP, the RTP
   * is cancelled. If they match, the message is silently ignored.
   *
   * @param rtp the RTP to validate or update
   * @param gdpMessage the GDP message that triggered the update
   * @return a {@link Mono} emitting the updated RTP if cancelled, or an empty completion if skipped
   */
  @Override
  @NonNull
  protected Mono<Rtp> updateRtp(@NonNull final Rtp rtp, @NonNull final GdpMessage gdpMessage) {
      return Mono.just(rtp)
        .doFirst(() -> log.info("Start processing {} update. messageId={}, rtpId={}",
                gdpMessage.status(), gdpMessage.id(), rtp.resourceID().getId()))
        .flatMap(rtpToUpdate -> this.retrieveServiceProviderIdByPspTaxCode(gdpMessage.psp_tax_code()))
        .doOnNext(pspTaxCode -> log.debug("Resolved PSP taxCode {} to serviceProviderId {}",
                gdpMessage.psp_tax_code(), pspTaxCode))
        .filter(pspTaxCode -> !pspTaxCode.equals(rtp.serviceProviderDebtor()))
        .doOnNext(pspTaxCode -> log.info("PSP mismatch detected. Proceeding to cancel RTP {}",
                rtp.resourceID().getId()))
        .flatMap(pspTaxCode -> sendRTPService.cancelRtp(rtp))
        .doOnSuccess(rtpUpdated -> log.info("RTP cancelled successfully. rtpId {}",
                rtp.resourceID().getId()))
        .switchIfEmpty(Mono.defer(() -> {
            log.info("PSP is the same as the one used to send the RTP. Skipping processing. rtpId={}",
                    rtp.resourceID().getId());
            return Mono.empty();
        }))
        .doOnError(error -> log.error("Error handling a {} message for RTP {}: {}",
                gdpMessage.status(), rtp.resourceID().getId(), error.getMessage(), error));
  }
}
