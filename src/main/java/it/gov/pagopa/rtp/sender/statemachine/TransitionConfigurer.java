package it.gov.pagopa.rtp.sender.statemachine;

import java.util.List;
import java.util.function.UnaryOperator;
import reactor.core.publisher.Mono;

public interface TransitionConfigurer<T, S, E> {

  TransitionConfigurer<T, S, E> register(TransitionKey<S, E> transitionKey, S toState);
  TransitionConfigurer<T, S, E> register(TransitionKey<S, E> transitionKey, S toState, UnaryOperator<Mono<T>> action);
  TransitionConfigurer<T, S, E> register(
      TransitionKey<S, E> transitionKey,
      S toState,
      List<UnaryOperator<Mono<T>>> preTransitionAction,
      List<UnaryOperator<Mono<T>>> postTransitionAction);

  TransitionConfiguration<T, S, E> build();

}
