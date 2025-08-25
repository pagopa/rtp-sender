package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import java.util.List;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

public class UpdateValidOperationException extends UpdateOperationProcessor {

  private static final List<RtpStatus> ACCEPTED_STATUSES = List.of(
      RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED
  );

  private static final List<GdpMessage.Status> SUPPORTED_STATUSES =
      List.of(Status.VALID);


  /**
   * Constructs a new {@code UpdateOperationProcessor} with required dependencies.
   *
   * @param registryDataService   the service for accessing registry data; must not be {@code null}
   * @param sendRTPService        the service for sending or retrieving RTPs; must not be {@code null}
   * @param gdpEventHubProperties the configuration properties for the Event Hub; must not be {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  protected UpdateValidOperationException(
      @NonNull final RegistryDataService registryDataService,
      @NonNull final SendRTPServiceImpl sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    super(registryDataService, sendRTPService, gdpEventHubProperties,
        ACCEPTED_STATUSES, SUPPORTED_STATUSES);
  }


  @NonNull
  @Override
  protected Mono<Rtp> updateRtp(@NonNull final Rtp rtp, @NonNull final GdpMessage gdpMessage) {
    return Mono.error(new UnsupportedOperationException("Update VALID existing RTP is not supported yet"));
  }


  @NonNull
  @Override
  protected Mono<Rtp> handleMissingRtp(@NonNull final Throwable cause,
      @NonNull final GdpMessage gdpMessage) {

    return Mono.error(new UnsupportedOperationException("Handle missing RTP for Update VALID operation is not supported yet"));
  }
}
