package it.gov.pagopa.rtp.sender.statemachine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Event;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RtpStateMachineTest {

  @Mock
  private TransitionConfiguration<RtpEntity, RtpStatus, RtpEvent> transitionConfiguration;

  @Mock
  private Transition<RtpEntity, RtpStatus, RtpEvent> transition;

  private RtpStateMachine stateMachine;

  private final RtpEntity rtp = new RtpEntity();
  private final RtpStatus sourceStatus = RtpStatus.CREATED;
  private final RtpEvent event = RtpEvent.SEND_RTP;
  private final RtpTransitionKey transitionKey = new RtpTransitionKey(sourceStatus, event);

  @BeforeEach
  void setUp() {
    rtp.setStatus(sourceStatus);
    rtp.setEvents(List.of(
        Event.builder()
            .timestamp(Instant.now())
            .triggerEvent(RtpEvent.CREATE_RTP)
            .build()
    ));
    stateMachine = new RtpStateMachine(transitionConfiguration);
  }

  @Test
  void givenValidTransition_whenCanTransition_thenReturnTrue() {
    when(transitionConfiguration.getTransition(transitionKey))
        .thenReturn(Optional.of(transition));

    StepVerifier.create(stateMachine.canTransition(rtp, event))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void givenInvalidTransition_whenCanTransition_thenReturnFalse() {
    when(transitionConfiguration.getTransition(transitionKey))
        .thenReturn(Optional.empty());

    StepVerifier.create(stateMachine.canTransition(rtp, event))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void givenValidTransition_whenTransition_thenApplyAndReturnEntity() {
    final var destination = RtpStatus.SENT;
    final var triggerEvent = RtpEvent.SEND_RTP;

    when(transitionConfiguration.getTransition(transitionKey))
        .thenReturn(Optional.of(transition));

    when(transition.getPreTransactionActions()).thenReturn(List.of(
        entity -> Mono.just(entity)
            .map(e -> {
              e.setPayeeName("pre-action");
              return e;
            }),
        entity -> Mono.just(entity)
            .map(e -> {
              e.setPayeeId("pre-action");
              return e;
            })));

    when(transition.getDestination()).thenReturn(destination);

    when(transition.getEvent()).thenReturn(triggerEvent);

    when(transition.getPostTransactionActions()).thenReturn(List.of(
        entity -> Mono.just(entity)
            .map(e -> {
              e.setPayerName("post-action");
              return e;
            }),
        entity -> Mono.just(entity)
            .map(e -> {
              e.setPayerId("post-action");
              return e;
            })));

    StepVerifier.create(stateMachine.transition(rtp, event))
        .assertNext(result -> {
          assertEquals(destination, result.getStatus());
          assertEquals("pre-action", result.getPayeeId());
          assertEquals("pre-action", result.getPayeeName());
          assertEquals("post-action", result.getPayerId());
          assertEquals("post-action", result.getPayerName());
          assertEquals(sourceStatus, result.getEvents().getLast().precStatus());
          assertEquals(triggerEvent, result.getEvents().getLast().triggerEvent());
        })
        .verifyComplete();
  }

  @Test
  void givenInvalidTransition_whenTransition_thenThrowIllegalStateException() {
    when(transitionConfiguration.getTransition(transitionKey))
        .thenReturn(Optional.empty());

    StepVerifier.create(stateMachine.transition(rtp, event))
        .expectErrorSatisfies(e -> {
          assertInstanceOf(IllegalStateException.class, e);
          assertTrue(e.getMessage().contains("Cannot transition from"));
        })
        .verify();
  }

  @ParameterizedTest
  @MethodSource("provideNullInputsForCanTransitionAndTransition")
  void givenNullInputs_whenInvokingMethods_thenThrowNullPointerException(
      String description,
      BiFunction<RtpStateMachine, RtpEvent, Publisher<?>> methodInvoker
  ) {
    final var rtpEntity = new RtpEntity();
    rtpEntity.setStatus(RtpStatus.CREATED);

    assertThrows(NullPointerException.class, () -> methodInvoker.apply(stateMachine, RtpEvent.SEND_RTP));
  }

    @Test
    void givenValidTransitionWithContext_whenTransition_thenIncludeContextInEvent() {
        final var destination = RtpStatus.SENT;
        final var triggerEvent = RtpEvent.SEND_RTP;

        when(transitionConfiguration.getTransition(transitionKey))
                .thenReturn(Optional.of(transition));

        when(transition.getPreTransactionActions()).thenReturn(List.of());
        when(transition.getDestination()).thenReturn(destination);
        when(transition.getEvent()).thenReturn(triggerEvent);
        when(transition.getPostTransactionActions()).thenReturn(List.of());

        final var expectedForeignStatus = GdpMessage.Status.VALID;
        final var expectedDispatcher = "dispatcher-test";

        StepVerifier.create(
                Mono.deferContextual(ctx ->
                                Mono.from(stateMachine.transition(rtp, event))
                ).contextWrite(ctx -> ctx
                        .put("foreignStatus", GdpMessage.Status.VALID)
                        .put("eventDispatcher", "dispatcher-test"))
                )
                .assertNext(result -> {
                    Event lastEvent = result.getEvents().getLast();
                    assertEquals(expectedForeignStatus, lastEvent.foreignStatus());
                    assertEquals(expectedDispatcher, lastEvent.eventDispatcher());
                    assertEquals(sourceStatus, lastEvent.precStatus());
                    assertEquals(triggerEvent, lastEvent.triggerEvent());
                })
                .verifyComplete();
    }

    @Test
    void givenValidTransitionWithoutContext_whenTransition_thenEventFieldsAreNull() {
        final var destination = RtpStatus.SENT;
        final var triggerEvent = RtpEvent.SEND_RTP;

        when(transitionConfiguration.getTransition(transitionKey))
                .thenReturn(Optional.of(transition));
        when(transition.getPreTransactionActions()).thenReturn(List.of());
        when(transition.getDestination()).thenReturn(destination);
        when(transition.getEvent()).thenReturn(triggerEvent);
        when(transition.getPostTransactionActions()).thenReturn(List.of());

        StepVerifier.create(stateMachine.transition(rtp, event))
                .assertNext(result -> {
                    Event lastEvent = result.getEvents().getLast();
                    assertEquals(sourceStatus, lastEvent.precStatus());
                    assertEquals(triggerEvent, lastEvent.triggerEvent());
                    assertNull(lastEvent.foreignStatus());
                    assertNull(lastEvent.eventDispatcher());
                })
                .verifyComplete();
    }

    private static Stream<Arguments> provideNullInputsForCanTransitionAndTransition() {
    return Stream.of(
        Arguments.of("canTransition(null, event)", (BiFunction<RtpStateMachine, RtpEvent, Publisher<?>>) (sm, ev) -> sm.canTransition(null, ev)),
        Arguments.of("canTransition(source, null)", (BiFunction<RtpStateMachine, RtpEvent, Publisher<?>>) (sm, ev) -> {
          final var source = new RtpEntity();
          source.setStatus(RtpStatus.CREATED);
          return sm.canTransition(source, null);
        }),
        Arguments.of("transition(null, event)", (BiFunction<RtpStateMachine, RtpEvent, Publisher<?>>) (sm, ev) -> sm.transition(null, ev)),
        Arguments.of("transition(source, null)", (BiFunction<RtpStateMachine, RtpEvent, Publisher<?>>) (sm, ev) -> {
          final var source = new RtpEntity();
          source.setStatus(RtpStatus.CREATED);
          return sm.transition(source, null);
        })
    );
  }

}
