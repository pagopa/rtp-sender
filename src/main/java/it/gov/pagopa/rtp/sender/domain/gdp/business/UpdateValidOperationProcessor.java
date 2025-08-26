package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.errors.RtpNotFoundException;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Status;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;


@Slf4j
public class UpdateValidOperationProcessor extends UpdateOperationProcessor {

  private static final List<RtpStatus> ACCEPTED_STATUSES = List.of(
      RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED
  );

  private static final List<GdpMessage.Status> SUPPORTED_STATUSES =
      List.of(Status.VALID);


  private final GdpMapper gdpMapper;


  /**
   * Constructs a new {@code UpdateOperationProcessor} with required dependencies.
   *
   * @param gdpMapper             the mapper for GDP messages; must not be {@code null}
   * @param sendRTPService        the service for sending or retrieving RTPs; must not be {@code null}
   * @param gdpEventHubProperties the configuration properties for the Event Hub; must not be {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  protected UpdateValidOperationProcessor(
      @NonNull final GdpMapper gdpMapper,
      @NonNull final SendRTPServiceImpl sendRTPService,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    super(sendRTPService, gdpEventHubProperties,
        ACCEPTED_STATUSES, SUPPORTED_STATUSES);

    this.gdpMapper = Objects.requireNonNull(gdpMapper);
  }


  @NonNull
  @Override
  protected Mono<Rtp> updateRtp(@NonNull final Rtp rtp, @NonNull final GdpMessage gdpMessage) {
    return Mono.error(new UnsupportedOperationException("Update VALID existing RTP is not supported yet"));
  }


  @NonNull
  @Override
  protected Mono<Rtp> handleMissingRtp(
      @NonNull final Throwable cause,
      @NonNull final GdpMessage gdpMessage) {

    return Mono.<Rtp>error(cause)
        .onErrorResume(RtpNotFoundException.class,
            ex -> Mono.just(gdpMessage)

                .doOnNext(message -> log.info("Creating new RTP. Operation ID: {}", message.id()))
                .mapNotNull(this.gdpMapper::toRtp)
                .doOnNext(rtp -> log.info("RTP created. ResourceId: {}", rtp.resourceID().getId()))

                .doOnNext(rtp -> log.info("Sending RTP. ResourceId: {}", rtp.resourceID().getId()))
                .flatMap(this.sendRTPService::send)

                .doOnSuccess(rtp -> log.info("RTP sent. ResourceId: {}", rtp.resourceID().getId())))
                .doOnError(ex -> log.error("Error sending RTP. ResourceId: {}", gdpMessage.id(), ex));
  }
}
