package it.gov.pagopa.rtp.sender.statemachine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpEntity;
import reactor.core.publisher.Mono;

class RtpTransitionConfigurerTest {

  private RtpTransitionConfigurer configurer;

  @BeforeEach
  void setUp() {
    configurer = new RtpTransitionConfigurer();
  }

  @Test
  void givenTransitionKeyAndTargetState_whenRegister_thenStoreTransition() {
    final var key = new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.SEND_RTP);
    final var toState = RtpStatus.SENT;

    configurer.register(key, toState);
    final var result = configurer.build().getTransition(key);

    assertTrue(result.isPresent());
    assertEquals(toState, result.get().getDestination());
  }

  @Test
  void givenTransitionKeyStateAndAction_whenRegister_thenStoreTransitionWithPostAction() {
    final var key = new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.SEND_RTP);
    final var toState = RtpStatus.SENT;
    final UnaryOperator<Mono<RtpEntity>> action = UnaryOperator.identity();

    configurer.register(key, toState, action);
    final var result = configurer.build().getTransition(key);

    assertTrue(result.isPresent());
    assertEquals(toState, result.get().getDestination());
    assertEquals(1, result.get().getPostTransactionActions().size());
    assertTrue(result.get().getPostTransactionActions().contains(action));
  }

  @Test
  void givenAllParameters_whenRegister_thenStoreTransitionWithAllActions() {
    final var key = new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.SEND_RTP);
    final var toState = RtpStatus.SENT;
    final UnaryOperator<Mono<RtpEntity>> preAction = UnaryOperator.identity();
    final UnaryOperator<Mono<RtpEntity>> postAction = UnaryOperator.identity();

    configurer.register(key, toState, List.of(preAction), List.of(postAction));
    final var result = configurer.build().getTransition(key);

    assertTrue(result.isPresent());
    assertEquals(toState, result.get().getDestination());
    assertEquals(List.of(preAction), result.get().getPreTransactionActions());
    assertEquals(List.of(postAction), result.get().getPostTransactionActions());
  }

  @Test
  void givenNoRegistrations_whenBuild_thenReturnEmptyConfiguration() {
    final var key = new RtpTransitionKey(RtpStatus.SENT, RtpEvent.ACCEPT_RTP);
    final var transition = configurer.build().getTransition(key);
    assertTrue(transition.isEmpty());
  }

}
