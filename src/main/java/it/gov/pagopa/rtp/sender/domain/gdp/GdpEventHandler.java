package it.gov.pagopa.rtp.sender.domain.gdp;

import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
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

        .onErrorContinue(this::handleError)

        .doOnError(error -> log.error("Exception found", error))
        .then();
  }


  /**
   * Handles errors that occur during message processing by logging the error details
   * along with context-specific information based on the type of the payload.
   *
   * <p>If the payload is an instance of {@link GdpMessage}, the GDP message ID is logged.</p>
   * <p>If the payload is an instance of {@link Rtp}, the RTP resource ID is logged.</p>
   * <p>If the payload is of an unknown type, only the error is logged without additional context.</p>
   *
   * @param error   the exception that occurred during processing (must not be {@code null})
   * @param context the message context associated with the error
   * @throws NullPointerException if {@code error} or {@code payload} is {@code null}
   */
  @NonNull
  private void handleError(
      @NonNull final Throwable error,
      @Nullable final Object context) {

    Objects.requireNonNull(error, "Error cannot be null");

    Optional.ofNullable(context)
        .ifPresentOrElse(ctx -> {
              switch (ctx) {
                case GdpMessage gdpMessage ->
                    log.error("Error processing message: GDP id: {}", gdpMessage.id(), error);

                case Rtp rtp ->
                    log.error("Error processing message: ResourceId: {}", rtp.resourceID().getId(),
                        error);

                default -> log.error("Error processing message.", error);
              }
            },
            () -> log.error("Error processing message.", error));
  }

}

