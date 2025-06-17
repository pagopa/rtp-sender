package it.gov.pagopa.rtp.sender.domain.gdp;

import it.gov.pagopa.rtp.sender.domain.gdp.business.OperationProcessorFactory;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@Component("gdpMessageProcessor")
@Slf4j
public class GdpMessageProcessor implements MessageProcessor<GdpMessage, Mono<Rtp>> {

  private final OperationProcessorFactory operationProcessorFactory;


  public GdpMessageProcessor(
      @NonNull final OperationProcessorFactory operationProcessorFactory) {
    this.operationProcessorFactory = Objects.requireNonNull(operationProcessorFactory);
  }


  @Override
  @NonNull
  public Mono<Rtp> processMessage(@NonNull final GdpMessage message) {
    Objects.requireNonNull(message, "GdpMessage cannot be null");

    return Mono.just(message)
        .doOnNext(payload -> log.info("Operation: {}", payload.operation()))
        .flatMap(payload -> this.operationProcessorFactory.getProcessor(payload)
            .flatMap(operationProcessor -> operationProcessor.processOperation(payload)));
  }
}
