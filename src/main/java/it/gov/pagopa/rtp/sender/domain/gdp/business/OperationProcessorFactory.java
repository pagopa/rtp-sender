package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@Component("operationProcessorFactory")
@Slf4j
public class OperationProcessorFactory {

  private final GdpMapper gdpMapper;
  private final SendRTPService sendRTPService;


  public OperationProcessorFactory(
      @NonNull final GdpMapper gdpMapper,
      @NonNull final SendRTPService sendRTPService) {

    this.gdpMapper = Objects.requireNonNull(gdpMapper);
    this.sendRTPService = Objects.requireNonNull(sendRTPService);
  }


  @NonNull
  public Mono<OperationProcessor> getProcessor(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "GdpMessage cannot be null");

    return Mono.just(gdpMessage)
        .map(GdpMessage::operation)
        .mapNotNull(this::createProcessorInstance)
        .switchIfEmpty(
            Mono.error(new UnsupportedOperationException(gdpMessage.operation().toString())));
  }


  @NonNull
  private OperationProcessor createProcessorInstance(@NonNull final Operation operation) {
    Objects.requireNonNull(operation, "Operation cannot be null");

    return switch (operation) {
      case CREATE -> new CreateOperationProcessor(this.gdpMapper, this.sendRTPService);
      default -> throw new UnsupportedOperationException(operation.toString());
    };
  }

}
