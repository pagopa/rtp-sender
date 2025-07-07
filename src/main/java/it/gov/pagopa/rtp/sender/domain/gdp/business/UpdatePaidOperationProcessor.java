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


/**
 * {@link OperationProcessor} implementation for handling {@link Status#PAID} update operations.
 * <p>
 * This processor validates the RTP status and performs different actions depending on whether
 * the debtor service provider matches the one retrieved from the PSP tax code in the message.
 * </p>
 *
 * @see UpdateOperationProcessor
 * @see Status#PAID
 * @see Rtp
 * @see GdpMessage
 */
@Slf4j
public class UpdatePaidOperationProcessor extends UpdateOperationProcessor {

  private static final List<RtpStatus> ACCEPTED_STATUSES = List.of(
      RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED
  );


  /**
   * Constructs a new {@code UpdatePaidOperationProcessor}.
   *
   * @param registryDataService   the registry data service used to resolve service provider IDs; must not be {@code null}
   * @param sendRTPService        the service for retrieving and updating RTPs; must not be {@code null}
   * @param gdpEventHubProperties the GDP Event Hub configuration; must not be {@code null}
   */
  public UpdatePaidOperationProcessor(
      @NonNull final RegistryDataService registryDataService,
      @NonNull final SendRTPService sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    super(
        registryDataService, sendRTPService, gdpEventHubProperties, ACCEPTED_STATUSES, Status.PAID);
  }


  /**
   * Updates given RTP based on the service provider matching logic.
   * <p>
   * If the debtor service provider in the RTP matches the PSP tax code in the message,
   * {@link #handleSamePsp(Rtp)} is called. Otherwise, {@link #handleDifferentPsp(Rtp)} is invoked.
   * </p>
   *
   * @param rtp        the RTP to update; must not be {@code null}
   * @param gdpMessage the GDP message providing update context; must not be {@code null}
   * @return a {@link Mono} emitting the updated RTP or an error if unsupported
   */
  @Override
  @NonNull
  protected Mono<Rtp> updateRtp(
      @NonNull final Rtp rtp, @NonNull final GdpMessage gdpMessage) {

    return this.retrieveServiceProviderIdByPspTaxCode(gdpMessage.pspTaxCode())
        .filter(pspBic -> pspBic.equals(rtp.serviceProviderDebtor()))
        .flatMap(pspBic -> this.handleSamePsp(rtp))
        .switchIfEmpty(Mono.fromDirect(this.handleDifferentPsp(rtp)));
  }


  /**
   * Handles updates for RTPs where the PSP tax code matches the debtor service provider.
   *
   * @param rtp the RTP with matching PSP and debtor service provider; must not be {@code null}
   * @return a {@link Mono} signaling the result of the handling
   * @throws UnsupportedOperationException always, as this method is not yet implemented
   */
  @NonNull
  private Mono<Rtp> handleSamePsp(@NonNull final Rtp rtp) {
    return Mono.error(new UnsupportedOperationException("Not supported yet"));
  }


  /**
   * Handles updates for RTPs where the PSP tax code does not match the debtor service provider.
   *
   * @param rtp the RTP with differing PSP and debtor service provider; must not be {@code null}
   * @return a {@link Mono} signaling the result of the handling
   * @throws UnsupportedOperationException always, as this method is not yet implemented
   */
  @NonNull
  private Mono<Rtp> handleDifferentPsp(@NonNull final Rtp rtp) {
    return Mono.error(new UnsupportedOperationException("Not supported yet"));
  }
}
