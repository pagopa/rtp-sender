package it.gov.pagopa.rtp.sender.domain.gdp;

import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Configuration class for handling GDP (Generic Digital Payment) messages from a Kafka topic.
 * This class defines a reactive message consumer that processes incoming GDP messages,
 * logs their details, and handles any processing errors.
 *
 * <p>The consumer is registered as a Spring Cloud Function with binding name "gdpMessageConsumer".
 * It processes messages in a reactive stream using Project Reactor's Flux and Mono types.</p>
 */
@Configuration("gdpEventHandler")
@RegisterReflectionForBinding(GdpMessage.class)
@Slf4j
public class GdpEventHandler {

  private final GdpMapper gdpMapper;


  public GdpEventHandler(@NonNull final GdpMapper gdpMapper) {
    this.gdpMapper = Objects.requireNonNull(gdpMapper);
  }


  /**
   * Creates a reactive function for consuming GDP messages from a Kafka topic.
   *
   * <p>The function performs the following operations for each message:
   * <ol>
   *   <li>Logs message metadata (partition, offset, timestamp)</li>
   *   <li>Logs the complete message payload</li>
   *   <li>Handles any errors that occur during processing</li>
   * </ol>
   * </p>
   *
   * <p>The function completes when the input stream completes (Mono<Void>).</p>
   *
   * @return A reactive function that consumes a Flux of GDP messages and returns a Mono<Void>
   *         to signal completion. The function is registered with the name "gdpMessageConsumer".
   *
   * @see org.springframework.messaging.Message
   * @see reactor.core.publisher.Flux
   * @see reactor.core.publisher.Mono
   *
   * @throws NullPointerException if the input message flux is null (enforced by @NonNull)
   *
   * @implNote The function uses the following Kafka headers if present:
   *           - {@link org.springframework.kafka.support.KafkaHeaders#PARTITION}
   *           - {@link org.springframework.kafka.support.KafkaHeaders#OFFSET}
   *           - {@link org.springframework.kafka.support.KafkaHeaders#TIMESTAMP}
   */
  @Bean("gdpMessageConsumer")
  @NonNull
  public Function<Flux<Message<GdpMessage>>, Mono<Void>> gdpMessageConsumer() {
    return gdpMessage -> gdpMessage
        .doOnNext(message -> log.info(
            "New GDP message received. partition: {}, offset: {}, enqueued time: {}",
            message.getHeaders().get(KafkaHeaders.PARTITION),
            message.getHeaders().get(KafkaHeaders.OFFSET),
            message.getHeaders().get(KafkaHeaders.TIMESTAMP)
        ))

        .map(Message::getPayload)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("No GDP payload found")))
        .doOnNext(payload -> log.info("Payload: {}", payload))

        .doOnNext(payload -> log.info("Mapping GDP payload to RTP"))
        .mapNotNull(this.gdpMapper::toRtp)

        .doOnNext(rtp -> log.info("RTP created. Resource Id {}", rtp.resourceID().getId()))
        .doOnError(error -> log.error("Exception found", error))
        .then();
  }
}
