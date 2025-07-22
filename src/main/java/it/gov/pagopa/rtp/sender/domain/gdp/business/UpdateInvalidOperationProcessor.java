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
 * Processor implementation for handling GDP messages with status {@link GdpMessage.Status#INVALID}.
 * <p>
 * This processor checks whether the PSP tax code in the message matches the RTP's recorded service provider debtor.
 * If the PSP differs, it cancels the RTP. Otherwise, the message is discarded.
 */
@Slf4j
public class UpdateInvalidOperationProcessor extends UpdateOperationProcessor {

  private static final List<RtpStatus> VALID_STATUSES =
      List.of(RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED);

  /**
  * Constructs the processor with required dependencies.
  *
  * @param registryDataService     service for retrieving service provider data from the registry
  * @param sendRTPService          service responsible for sending or cancelling RTPs
  * @param gdpEventHubProperties   configuration properties for GDP event hub
  */
  public UpdateInvalidOperationProcessor(
      @NonNull final RegistryDataService registryDataService,
      @NonNull final SendRTPServiceImpl sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    super(registryDataService, sendRTPService, gdpEventHubProperties, VALID_STATUSES, GdpMessage.Status.INVALID);
  }

   /**
   * Processes an INVALID GDP message.
   * <p>
   * If the PSP tax code in the message differs from the one in the RTP, the RTP is cancelled.
   * Otherwise, processing is skipped.
   *
   * @param rtp         the RTP to be checked or updated
   * @param gdpMessage  the GDP message triggering the update
   * @return a {@link Mono} emitting the updated RTP if it was cancelled, or completing empty if skipped
   */
  @Override
  @NonNull
  protected Mono<Rtp> updateRtp(@NonNull final Rtp rtp, @NonNull final GdpMessage gdpMessage) {
      return Mono.just(rtp)
        .doFirst(() -> log.info("Start processing INVALID update. messageId={}, rtpId={}",
                gdpMessage.id(), rtp.resourceID().getId()))
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
        .doOnError(error -> log.error("Error handling an INVALID message for RTP {}: {}",
                rtp.resourceID().getId(), error.getMessage(), error));
  }
}
