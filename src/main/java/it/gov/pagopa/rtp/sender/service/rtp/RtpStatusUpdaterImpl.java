package it.gov.pagopa.rtp.sender.service.rtp;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpEntity;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpMapper;
import it.gov.pagopa.rtp.sender.statemachine.StateMachine;
import it.gov.pagopa.rtp.sender.statemachine.StateMachineFactory;
import reactor.core.publisher.Mono;


/**
 * Default implementation of the {@link RtpStatusUpdater} interface.
 * <p>
 * This component provides state transition logic for {@link Rtp} instances using a state machine.
 * Each public method corresponds to a domain event that can be triggered on an {@link Rtp} instance,
 * such as sending, cancelling, or accepting a Request-to-Pay.
 * <p>
 */
@Component("rtpStatusUpdater")
@Slf4j
public class RtpStatusUpdaterImpl implements RtpStatusUpdater {

  private final StateMachine<RtpEntity, RtpEvent> stateMachine;
  private final RtpMapper rtpMapper;


  /**
   * Constructs an {@code RtpStatusUpdaterImpl} with required dependencies.
   *
   * @param stateMachineFactory factory for creating a state machine instance
   * @param rtpMapper           mapper used to convert between {@link Rtp} and {@link RtpEntity}
   */
  public RtpStatusUpdaterImpl(
      @NonNull final StateMachineFactory<RtpEntity, RtpEvent> stateMachineFactory,
      @NonNull final RtpMapper rtpMapper) {

    this.stateMachine = Objects.requireNonNull(stateMachineFactory)
        .createStateMachine();
    this.rtpMapper = Objects.requireNonNull(rtpMapper);
  }


  /**
   * Triggers the {@code SEND_RTP} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerSendRtp(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.SEND_RTP);
  }


  /**
   * Triggers the {@code CANCEL_RTP} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerCancelRtp(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.CANCEL_RTP);
  }


  /**
   * Triggers the {@code ACCEPT_RTP} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerAcceptRtp(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.ACCEPT_RTP);
  }


  /**
   * Triggers the {@code REJECT_RTP} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerRejectRtp(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.REJECT_RTP);
  }


  /**
   * Triggers the {@code USER_ACCEPT_RTP} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerUserAcceptRtp(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.USER_ACCEPT_RTP);
  }


  /**
   * Triggers the {@code USER_REJECT_RTP} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerUserRejectRtp(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.USER_REJECT_RTP);
  }


  /**
   * Triggers the {@code PAY_RTP} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerPayRtp(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.PAY_RTP);
  }


  /**
   * Triggers the {@code ERROR_SEND_RTP} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerErrorSendRtp(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.ERROR_SEND_RTP);
  }


  /**
   * Triggers the {@code ERROR_CANCEL_RTP} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerErrorCancelRtp(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.ERROR_CANCEL_RTP);
  }


  /**
   * Triggers the {@code CANCEL_RTP_ACCR} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerCancelRtpAccr(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.CANCEL_RTP_ACCR);
  }


  /**
   * Triggers the {@code CANCEL_RTP_REJECTED} event on the given RTP.
   */
  @NonNull
  @Override
  public Mono<Rtp> triggerCancelRtpRejected(@NonNull final Rtp rtp) {
    return this.triggerEvent(rtp, RtpEvent.CANCEL_RTP_REJECTED);
  }


  /**
   * Checks whether the specified {@link RtpEvent} can be triggered on the given RTP.
   */
  @Override
  public Mono<Boolean> canCancel(@NonNull final Rtp rtp) {
    return this.canTransition(rtp, RtpEvent.CANCEL_RTP);
  }


  /**
   * Triggers the given {@link RtpEvent} on the provided {@link Rtp} instance using the state machine.
   * <p>
   * The transition is performed by converting the domain model to its entity representation,
   * executing the transition, and then converting the result back.
   *
   * @param sourceRtp the RTP to transition
   * @param event     the event to trigger
   * @return a {@link Mono} emitting the transitioned {@link Rtp}
   */
  @NonNull
  private Mono<Rtp> triggerEvent(
      @NonNull final Rtp sourceRtp, @NonNull final RtpEvent event) {

    Objects.requireNonNull(sourceRtp, "Rtp cannot be null");
    Objects.requireNonNull(event, "Event cannot be null");

    return Mono.just(sourceRtp)
        .doFirst(() -> log.debug("Triggering event {} for RTP status {}", event, sourceRtp.status()))
        .doOnNext(rtp -> log.debug("Mapping RTP model to RTP entity."))
        .map(this.rtpMapper::toDbEntity)
        .doOnNext(rtp -> log.debug("Calling state machine."))
        .flatMap(rtpEntity ->
            Mono.from(this.stateMachine.transition(rtpEntity, event)))
        .doOnNext(rtp -> log.debug("Mapping RTP entity to RTP model."))
        .map(this.rtpMapper::toDomain);
  }

  /**
   * Checks if the given {@link RtpEvent} can be applied to the provided {@link Rtp} instance.
   * <p>
   * This method converts the domain model to its entity representation and queries the state machine
   * to determine if the transition is possible from the current state.
   *
   * @param sourceRtp the RTP to evaluate
   * @param event     the event representing the possible transition
   * @return a {@link Mono} emitting {@code true} if the transition is possible, {@code false} otherwise
   */
  @NonNull
  private Mono<Boolean> canTransition(
          @NonNull final Rtp sourceRtp,
          @NonNull final RtpEvent event) {

    Objects.requireNonNull(sourceRtp, "Rtp cannot be null");
    Objects.requireNonNull(event, "Event cannot be null");

    return Mono.just(sourceRtp)
            .doOnNext(rtp -> log.debug("Checking transition possibility for RTP id {} in status {} with event {}",
                    rtp.resourceID().getId(), rtp.status(), event))
            .map(this.rtpMapper::toDbEntity)
            .flatMap(rtpEntity -> Mono.from(this.stateMachine.canTransition(rtpEntity, event)))
            .doOnNext(canTransition -> log.debug("Can transition result for RTP id {} with event {}: {}",
                    sourceRtp.resourceID().getId(), event, canTransition));

  }
}

