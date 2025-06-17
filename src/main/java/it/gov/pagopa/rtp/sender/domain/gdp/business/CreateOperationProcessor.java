package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import java.util.Objects;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

public class CreateOperationProcessor implements OperationProcessor {

  private final GdpMapper gdpMapper;


  public CreateOperationProcessor(@NonNull final GdpMapper gdpMapper) {
    this.gdpMapper = Objects.requireNonNull(gdpMapper);
  }


  @Override
  @NonNull
  public Mono<Rtp> processOperation(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "gdpMessage must not be null");

    return Mono.just(gdpMessage)
        .mapNotNull(this.gdpMapper::toRtp);
  }
}
