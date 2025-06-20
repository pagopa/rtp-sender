package it.gov.pagopa.rtp.sender.configuration;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpDB;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpEntity;
import it.gov.pagopa.rtp.sender.statemachine.RtpTransitionConfigurer;
import it.gov.pagopa.rtp.sender.statemachine.RtpTransitionKey;
import it.gov.pagopa.rtp.sender.statemachine.TransitionConfigurer;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;


/**
 * Configuration class responsible for setting up the state machine transitions
 * for the {@link RtpEntity} domain model.
 * <p>
 * This configuration defines allowed transitions between different {@link RtpStatus} states
 * triggered by specific {@link RtpEvent} events. It also associates actions such as persisting
 * the {@link RtpEntity} after each state change.
 * </p>
 */
@Configuration
@Slf4j
public class StateMachineConfiguration {

  private final RtpDB rtpRepository;
  private final ServiceProviderConfig serviceProviderConfig;


  /**
   * Constructs a new {@code StateMachineConfiguration} with the given repository.
   *
   * @param rtpRepository the repository used to persist {@link RtpEntity} instances after transitions
   * @param serviceProviderConfig the configuration for the service provider
   */
  public StateMachineConfiguration(
      @NonNull final RtpDB rtpRepository,
      @NonNull final ServiceProviderConfig serviceProviderConfig) {
    this.rtpRepository = Objects.requireNonNull(rtpRepository);
    this.serviceProviderConfig = Objects.requireNonNull(serviceProviderConfig);
  }


  /**
   * Defines and registers all valid transitions for the RTP state machine.
   * <p>
   * Each transition maps a combination of source {@link RtpStatus} and triggering {@link RtpEvent}
   * to a target {@link RtpStatus}. For every transition, the entity is persisted after the status
   * change by default.
   * </p>
   *
   * @return a fully configured {@link TransitionConfigurer} for {@link RtpEntity} transitions
   */
  @Bean("transitionConfigurer")
  public TransitionConfigurer<RtpEntity, RtpStatus, RtpEvent> transitionConfigurer() {
    return new RtpTransitionConfigurer()
        // Transitions from CREATED
        .register(new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.SEND_RTP), RtpStatus.SENT, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.ERROR_SEND_RTP), RtpStatus.ERROR_SEND, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.ACCEPT_RTP), RtpStatus.ACCEPTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.REJECT_RTP), RtpStatus.REJECTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.USER_ACCEPT_RTP), RtpStatus.USER_ACCEPTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.USER_REJECT_RTP), RtpStatus.USER_REJECTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.PAY_RTP), RtpStatus.PAYED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.CANCEL_RTP), RtpStatus.CANCELLED, persistRtp())

        // Transitions from SENT
        .register(new RtpTransitionKey(RtpStatus.SENT, RtpEvent.ACCEPT_RTP), RtpStatus.ACCEPTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.SENT, RtpEvent.REJECT_RTP), RtpStatus.REJECTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.SENT, RtpEvent.USER_ACCEPT_RTP), RtpStatus.USER_ACCEPTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.SENT, RtpEvent.USER_REJECT_RTP), RtpStatus.USER_REJECTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.SENT, RtpEvent.PAY_RTP), RtpStatus.PAYED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.SENT, RtpEvent.CANCEL_RTP), RtpStatus.CANCELLED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.SENT, RtpEvent.ERROR_SEND_RTP), RtpStatus.ERROR_SEND, persistRtp())

        // Transitions from ACCEPTED
        .register(new RtpTransitionKey(RtpStatus.ACCEPTED, RtpEvent.USER_ACCEPT_RTP), RtpStatus.USER_ACCEPTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.ACCEPTED, RtpEvent.USER_REJECT_RTP), RtpStatus.USER_REJECTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.ACCEPTED, RtpEvent.CANCEL_RTP), RtpStatus.CANCELLED, persistRtp())

        // Transitions from USER_ACCEPTED
        .register(new RtpTransitionKey(RtpStatus.USER_ACCEPTED, RtpEvent.PAY_RTP), RtpStatus.PAYED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.USER_ACCEPTED, RtpEvent.CANCEL_RTP), RtpStatus.CANCELLED, persistRtp())

        // Transitions from CANCELLED
        .register(new RtpTransitionKey(RtpStatus.CANCELLED, RtpEvent.CANCEL_RTP_ACCR), RtpStatus.CANCELLED_ACCR, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.CANCELLED, RtpEvent.CANCEL_RTP_REJECTED), RtpStatus.CANCELLED_REJECTED, persistRtp())
        .register(new RtpTransitionKey(RtpStatus.CANCELLED, RtpEvent.ERROR_CANCEL_RTP), RtpStatus.ERROR_CANCEL, persistRtp());
  }


  /**
   * Helper method to create a persisting action for {@link RtpEntity} instances.
   *
   * @return a {@link Function} that persists the entity using {@link RtpDB}
   */
  private Function<RtpEntity, Mono<RtpEntity>> persistRtp() {
    return rtpEntity -> Mono.just(rtpEntity)
        .flatMap(rtpRepository::save)
        .retryWhen(retryPolicy());
  }


  /**
   * Builds a {@link RetryBackoffSpec} for persisting RTP entities using configuration values.
   *
   * @return a configured {@code RetryBackoffSpec} for retrying persistence operations
   */
  private RetryBackoffSpec retryPolicy() {
    final var maxAttempts = serviceProviderConfig.send().retry().maxAttempts();
    final var minDurationMillis = serviceProviderConfig.send().retry().backoffMinDuration();
    final var jitter = serviceProviderConfig.send().retry().backoffJitter();

    return Retry.backoff(maxAttempts, Duration.ofMillis(minDurationMillis))
        .jitter(jitter)
        .doAfterRetry(signal -> log.info("Retry number {}", signal.totalRetries()));
  }

}
