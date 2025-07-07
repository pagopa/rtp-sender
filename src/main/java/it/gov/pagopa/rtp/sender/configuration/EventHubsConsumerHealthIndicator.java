package it.gov.pagopa.rtp.sender.configuration;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component("eventHubsConsumerHealthIndicator")
@RequiredArgsConstructor
public class EventHubsConsumerHealthIndicator implements ReactiveHealthIndicator {

  private final ConsumerStarter consumerStarter;

  @Override
  public Mono<Health> health() {
    if (consumerStarter.isConsumerRunning()) {
      return Mono
          .just(Health.up().withDetail("message", "Consumer binding 'gdpMessageConsumer-in-0' is running").build());
    } else {
      return Mono.just(Health.down()
          .withDetail("message",
              "Consumer binding 'gdpMessageConsumer-in-0' is not running. Awaiting connection to Event Hubs.")
          .build());
    }
  }

}
