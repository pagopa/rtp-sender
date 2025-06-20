package it.gov.pagopa.rtp.sender.statemachine;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Validated
@RequiredArgsConstructor
@Getter
public abstract class Transition<T, S, E> {

  @NotNull
  private final S source;

  @NotNull
  private final E event;

  @NotNull
  private final S destination;

  @NotNull
  private final List<UnaryOperator<Mono<T>>> preTransactionActions;

  @NotNull
  private final List<UnaryOperator<Mono<T>>> postTransactionActions;

}
