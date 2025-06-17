package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;


@Slf4j
public class CreateOperationProcessor implements OperationProcessor {

  private final GdpMapper gdpMapper;
  private final SendRTPService sendRTPService;


  public CreateOperationProcessor(
      @NonNull final GdpMapper gdpMapper,
      @NonNull final SendRTPService sendRTPService) {

    this.gdpMapper = Objects.requireNonNull(gdpMapper);
    this.sendRTPService = Objects.requireNonNull(sendRTPService);
  }


  @Override
  @NonNull
  public Mono<Rtp> processOperation(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "gdpMessage must not be null");

    return Mono.just(gdpMessage)
        .doOnNext(message -> log.info("Mapping GDP message to RTP"))
        .mapNotNull(this.gdpMapper::toRtp)
        .doOnNext(rtp -> log.info("Sending RTP. ResourceId: {}", rtp.resourceID()))
        .flatMap(this.sendRTPService::send);
  }
}
