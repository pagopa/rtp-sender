package it.gov.pagopa.rtp.sender.domain.gdp;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
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
 * <p>
 * This class registers a Spring Cloud Function bean named {@code gdpMessageConsumer} that consumes
 * Kafka messages containing {@link GdpMessage} payloads. It uses a {@link GdpMapper} to transform
 * incoming GDP messages into RTP payloads and logs relevant processing details.
 * </p>
 *
 * <p>
 * The consumer leverages Project Reactor's {@link Flux} and {@link Mono} for non-blocking, asynchronous processing.
 * </p>
 */
@Configuration("gdpEventHandler")
@RegisterReflectionForBinding(GdpMessage.class)
@Slf4j
public class GdpEventHandler {

  private final MessageProcessor<GdpMessage, Mono<Rtp>> gdProcessor;


  /**
   * Constructs the GDP event handler with the provided mapper.
   *
   * @param gdpMapper The mapper responsible for transforming GDP messages to RTP format.
   * @throws NullPointerException if {@code gdpMapper} is null
   */
  public GdpEventHandler(
      @NonNull final MessageProcessor<GdpMessage, Mono<Rtp>> gdProcessor) {
    this.gdProcessor = Objects.requireNonNull(gdProcessor);
  }

  /**
   * Defines a Spring Cloud Stream consumer function that processes GDP messages from Kafka.
   *
   * <p>Each message is handled as follows:</p>
   * <ul>
   *   <li>Logs Kafka metadata headers such as partition, offset, and timestamp.</li>
   *   <li>Logs the full GDP message payload.</li>
   *   <li>Maps the GDP payload to an RTP representation using {@link GdpMapper}.</li>
   *   <li>Logs the resulting RTP object’s resource ID.</li>
   *   <li>Handles and logs any exceptions that occur during processing.</li>
   * </ul>
   *
   * <p>If the mapping returns {@code null}, an {@link IllegalStateException} is thrown.</p>
   *
   * @return A {@link Function} that consumes a {@link Flux} of Kafka {@link Message} objects
   *         with {@link GdpMessage} payloads and returns a {@link Mono<Void>} upon completion.
   *
   * @implNote This function is bound to the Spring Cloud Function binding named {@code gdpMessageConsumer}.
   * @see org.springframework.kafka.support.KafkaHeaders
   * @see GdpMessage
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
