package it.gov.pagopa.rtp.sender.domain.gdp;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@Component("gdpMessageProcessor")
@Slf4j
public class GdpMessageProcessor implements MessageProcessor<GdpMessage, Mono<Rtp>> {

  private final GdpMapper gdpMapper;
  private final SendRTPService sendRTPService;


  public GdpMessageProcessor(
      @NonNull final GdpMapper gdpMapper,
      @NonNull final SendRTPService sendRTPService) {
    this.gdpMapper = Objects.requireNonNull(gdpMapper);
    this.sendRTPService = Objects.requireNonNull(sendRTPService);
  }


  @Override
  @NonNull
  public Mono<Rtp> processMessage(@NonNull final GdpMessage message) {
    Objects.requireNonNull(message, "GdpMessage cannot be null");

    return Mono.just(message)
        .doOnNext(payload -> log.info("Operation: {}", payload.operation()))
        .filter(payload -> payload.operation().equals(Operation.CREATE))
        .switchIfEmpty(Mono.fromRunnable(() -> log.warn("Operation unsupported, skipping message")))

        .doOnNext(payload -> log.info("Mapping GDP payload to RTP"))
        .mapNotNull(this.gdpMapper::toRtp)

        .doOnNext(rtp -> log.info("Sending RTP: {}", rtp))
        .flatMap(this.sendRTPService::send);
  }
}
