package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;


/**
 * Processor responsible for handling {@link Operation#CREATE} GDP messages.
 *
 * <p>This implementation of {@link OperationProcessor} processes messages with a CREATE operation by:
 * <ul>
 *   <li>Mapping the {@link GdpMessage} to an {@link Rtp} object using {@link GdpMapper}.</li>
 *   <li>Sending the RTP using the {@link SendRTPService}.</li>
 * </ul>
 *
 * <p>If the mapping to RTP returns {@code null}, processing stops and the returned {@link Mono} completes empty.</p>
 *
 * @see OperationProcessor
 * @see Operation
 * @see GdpMapper
 * @see Rtp
 * @see SendRTPService
 */
@Slf4j
public class CreateOperationProcessor implements OperationProcessor {

  private final GdpMapper gdpMapper;
  private final SendRTPService sendRTPService;


  /**
   * Constructs a new {@code CreateOperationProcessor} with the required dependencies.
   *
   * @param gdpMapper       the mapper to convert GDP messages to RTP objects; must not be {@code null}
   * @param sendRTPService  the service used to send the resulting RTP; must not be {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  public CreateOperationProcessor(
      @NonNull final GdpMapper gdpMapper,
      @NonNull final SendRTPService sendRTPService) {

    this.gdpMapper = Objects.requireNonNull(gdpMapper);
    this.sendRTPService = Objects.requireNonNull(sendRTPService);
  }


  /**
   * Processes a {@link GdpMessage} with a CREATE operation.
   *
   * <p>Maps the message to an {@link Rtp} and sends it using the configured service.</p>
   *
   * @param gdpMessage the message to process; must not be {@code null}
   * @return a {@link Mono} emitting the sent {@link Rtp}, or completing empty if mapping returns {@code null}
   * @throws NullPointerException if {@code gdpMessage} is {@code null}
   */
  @Override
  @NonNull
  public Mono<Rtp> processOperation(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "gdpMessage must not be null");

    return Mono.just(gdpMessage)
        .filter(message -> message.status() == GdpMessage.Status.VALID)
        .switchIfEmpty(Mono.fromRunnable(() ->
                log.info("Skipping GDP message with id {} due to non-VALID status: {}", gdpMessage.id(), gdpMessage.status())
        ))
        .doOnNext(message -> log.info("Mapping GDP message to RTP"))
        .mapNotNull(this.gdpMapper::toRtp)
        .doOnNext(rtp -> log.info("Sending RTP. ResourceId: {}", rtp.resourceID()))
        .flatMap(this.sendRTPService::send);
  }
}

