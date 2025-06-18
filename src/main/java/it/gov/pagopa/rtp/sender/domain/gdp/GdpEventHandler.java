package it.gov.pagopa.rtp.sender.domain.gdp;

import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
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
 * Configuration class that defines the reactive Kafka consumer for GDP messages.
 *
 * <p>This class registers a Spring Cloud Function bean named {@code gdpMessageConsumer} that consumes
 * Kafka messages with {@link GdpMessage} payloads. The payloads are processed via a generic
 * {@link MessageProcessor}, which produces {@link Rtp} results in a reactive, non-blocking manner.</p>
 */
@Configuration("gdpEventHandler")
@RegisterReflectionForBinding(GdpMessage.class)
@Slf4j
public class GdpEventHandler {

  private final MessageProcessor<GdpMessage, Mono<Rtp>> gdProcessor;


  /**
   * Constructs a new {@link GdpEventHandler} with the provided {@link MessageProcessor}.
   *
   * @param gdProcessor the processor responsible for handling {@link GdpMessage} payloads
   * @throws NullPointerException if {@code gdProcessor} is {@code null}
   */
  public GdpEventHandler(
      @NonNull final MessageProcessor<GdpMessage, Mono<Rtp>> gdProcessor) {
    this.gdProcessor = Objects.requireNonNull(gdProcessor);
  }


  /**
   * Defines a Spring Cloud Stream consumer function named {@code gdpMessageConsumer} that processes
   * incoming Kafka messages with {@link GdpMessage} payloads.
   *
   * <p>Each message is processed by:</p>
   * <ul>
   *   <li>Logging Kafka metadata such as partition, offset, and timestamp.</li>
   *   <li>Logging the GDP message payload.</li>
   *   <li>Delegating message handling to the injected {@link MessageProcessor}.</li>
   *   <li>Handling errors gracefully and logging the failed {@link Rtp} context if possible.</li>
   * </ul>
   *
   * <p>Any errors encountered during processing are logged, but do not interrupt the stream.</p>
   *
   * @return a {@link Function} that takes a {@link Flux} of Kafka {@link Message} objects containing
   *         {@link GdpMessage} payloads and returns a {@link Mono<Void>} when the stream is consumed.
   *
   * @implNote This bean must be named {@code gdpMessageConsumer} to match the Spring Cloud Stream
   * binding configuration.
   *
   * @see MessageProcessor
   * @see GdpMessage
   * @see Rtp
   * @see KafkaHeaders
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
        .switchIfEmpty(Mono.fromRunnable(() -> log.warn("Payload is null")))
        .doOnNext(payload -> log.info("Payload: {}", payload))

        .flatMap(this.gdProcessor::processMessage)

        .onErrorContinue((e, rtp) ->
            log.error("Error processing message: ResourceId: {}", rtp, e))

        .doOnError(error -> log.error("Exception found", error))
        .then();
  }
}

