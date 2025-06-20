package it.gov.pagopa.rtp.sender.statemachine;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import reactor.core.publisher.Mono;

public interface TransitionConfigurer<T, S, E> {

  TransitionConfigurer<T, S, E> register(TransitionKey<S, E> transitionKey, S toState);
  TransitionConfigurer<T, S, E> register(TransitionKey<S, E> transitionKey, S toState, Function<T, Mono<T>> action);
  TransitionConfigurer<T, S, E> register(
      TransitionKey<S, E> transitionKey,
      S toState,
      List<Function<T, Mono<T>>> preTransitionAction,
      List<Function<T, Mono<T>>> postTransitionAction);

  TransitionConfiguration<T, S, E> build();

}
