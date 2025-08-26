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


/**
 * {@code UpdateValidOperationProcessor} is a concrete implementation of {@link UpdateOperationProcessor}
 * responsible for handling GDP messages with status {@link GdpMessage.Status#VALID}.
 * <p>
 * This processor supports updating RTPs only if their current status is within the
 * accepted states defined in {@link #ACCEPTED_STATUSES}.
 * <p>
 * The update of existing RTPs is not yet supported (see {@link #updateRtp(Rtp, GdpMessage)}),
 * but missing RTPs can be created and sent if an {@link RtpNotFoundException} occurs.
 */
@Slf4j
public class UpdateValidOperationProcessor extends UpdateOperationProcessor {

  /**
   * The set of RTP statuses that are eligible for an update in this processor.
   */
  private static final List<RtpStatus> ACCEPTED_STATUSES = List.of(
      RtpStatus.CREATED, RtpStatus.SENT, RtpStatus.ACCEPTED, RtpStatus.USER_ACCEPTED
  );

  /**
   * The set of GDP message statuses that this processor supports.
   */
  private static final List<GdpMessage.Status> SUPPORTED_STATUSES =
      List.of(Status.VALID);


  /**
   * Mapper used to convert {@link GdpMessage} instances into {@link Rtp} objects.
   */
  private final GdpMapper gdpMapper;


  /**
   * Constructs a new {@code UpdateValidOperationProcessor} with required dependencies.
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


  /**
   * Updates an existing RTP based on the given GDP message.
   * <p>
   * Currently not supported.
   *
   * @param rtp        the RTP to update; must not be {@code null}
   * @param gdpMessage the GDP message providing update information; must not be {@code null}
   * @return a {@link Mono} that always errors with {@link UnsupportedOperationException}
   */
  @NonNull
  @Override
  protected Mono<Rtp> updateRtp(@NonNull final Rtp rtp, @NonNull final GdpMessage gdpMessage) {
    return Mono.error(new UnsupportedOperationException("Update VALID existing RTP is not supported yet"));
  }


  /**
   * Handles the case where no RTP was found for the given GDP message.
   * <p>
   * If the cause is an {@link RtpNotFoundException}, a new RTP is created from the GDP message,
   * sent via the {@link SendRTPServiceImpl}, and the sent RTP is returned.
   * For other exceptions, the error is propagated.
   *
   * @param cause      the exception that occurred; must not be {@code null}
   * @param gdpMessage the GDP message that triggered the operation; must not be {@code null}
   * @return a {@link Mono} emitting the newly created and sent RTP if the cause is {@code RtpNotFoundException},
   * otherwise a {@link Mono#error(Throwable)}
   */
  @NonNull
  @Override
  protected Mono<Rtp> handleMissingRtp(
      @NonNull final Throwable cause,
      @NonNull final GdpMessage gdpMessage) {

    return Mono.<Rtp>error(cause)
        .onErrorResume(RtpNotFoundException.class,
            ex -> this.createAndSendRtp(gdpMessage)
                .doFirst(() -> log.warn(ex.getMessage())))

        .doOnError(ex -> log.error("Error sending RTP. ResourceId: {}", gdpMessage.id(), ex));
  }


  @NonNull
  private Mono<Rtp> createAndSendRtp(@NonNull final GdpMessage gdpMessage) {
    return Mono.just(gdpMessage)

        .doOnNext(message -> log.info("Creating new RTP. Operation ID: {}", message.id()))
        .mapNotNull(this.gdpMapper::toRtp)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Created Rtp cannot be null")))
        .doOnNext(rtp -> log.info("RTP created. ResourceId: {}", rtp.resourceID().getId()))

        .doOnNext(rtp -> log.info("Sending RTP. ResourceId: {}", rtp.resourceID().getId()))
        .flatMap(this.sendRTPService::send)
        .doOnNext(rtp -> log.info("RTP sent. ResourceId: {}", rtp.resourceID().getId()));
  }
}
