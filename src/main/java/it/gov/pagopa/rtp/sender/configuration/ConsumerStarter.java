package it.gov.pagopa.rtp.sender.configuration;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 * Component responsible for managing the lifecycle of the GDP message consumer binding.
 * 
 * <p>Due to the fact that the event hub queue could be not available at the startup 
 * of the application, we need to disable the auto-startup of the consumer. The consumer 
 * will be started manually by this class with retry logic to handle initial connection 
 * failures.</p>
 * 
 * <p>This class listens for the {@link ApplicationReadyEvent} and attempts to start 
 * the consumer binding with exponential backoff retry strategy. It also provides 
 * methods to check the consumer status and gracefully stop the consumer during 
 * application shutdown.</p>
 * 
 * @see BindingsLifecycleController
 * @see ApplicationReadyEvent
 */
@Slf4j
@Component
public class ConsumerStarter {

  private static final String BINDING_NAME = "gdpMessageConsumer-in-0";
  private final BindingsLifecycleController bindingsLifecycleController;

  private final RetryBackoffSpec retrySpec = Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(10))
      .maxBackoff(Duration.ofMinutes(3))
      .jitter(0.5);

  public ConsumerStarter(BindingsLifecycleController bindingsLifecycleController) {
    this.bindingsLifecycleController = bindingsLifecycleController;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void startConsumerWithRetries() {
    log.info("Application is ready, starting consumer: {}", BINDING_NAME);

    Mono.fromRunnable(() -> {
      log.info("Attempting to start consumer binding '{}'...", BINDING_NAME);
      bindingsLifecycleController.changeState(BINDING_NAME, BindingsLifecycleController.State.STARTED);
    })
        .doOnSuccess(
            v -> log.info("Consumer binding '{}' started successfully. Your gdpMessageConsumer function in now active.",
                BINDING_NAME))
        .retryWhen(retrySpec.doBeforeRetry(retrySignal -> {
          log.warn("Failed to start consumer binding '{}'. Will retry. Attempt: #{}. Cause: {}}",
              BINDING_NAME,
              retrySignal.totalRetries() + 1,
              retrySignal.failure().getMessage());
        }))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
  }

  public boolean isConsumerRunning() {
    try {
      List<Binding<?>>  bindings = bindingsLifecycleController.queryState(BINDING_NAME);
      if (bindings == null || bindings.isEmpty()) {
        return false;
      }
      return bindings.stream().anyMatch(Binding::isRunning);
    } catch (Exception e) {
      return false;
    }
  }

  @PreDestroy
  public void stopConsumer() {
    log.info("Application is shutting down, stopping consumer: {}", BINDING_NAME);
    try {
      bindingsLifecycleController.changeState(BINDING_NAME, BindingsLifecycleController.State.STOPPED);
    } catch (Exception e) {
      log.warn("Could not gracefully stop consumer binding '{}'.", BINDING_NAME, e);
    }
  }

}
