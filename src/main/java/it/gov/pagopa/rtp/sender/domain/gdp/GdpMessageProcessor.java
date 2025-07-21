package it.gov.pagopa.rtp.sender.domain.gdp;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.gdp.business.OperationProcessor;
import it.gov.pagopa.rtp.sender.domain.gdp.business.OperationProcessorFactory;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Message processor responsible for handling incoming {@link GdpMessage} instances by delegating
 * the processing to an appropriate {@link OperationProcessor} based on the message's {@link Operation} type.
 *
 * <p>This component enables dynamic routing of processing logic, supporting extensibility for multiple operation types
 * (e.g., {@link Operation#CREATE}, {@link Operation#UPDATE}, etc.),
 * each handled by a different implementation of {@link OperationProcessor}.</p>
 *
 * <p>The processing is asynchronous and returns a {@link Mono} containing the resulting {@link Rtp} instance,
 * or an error if the operation is unsupported or if downstream processing fails.</p>
 *
 * @see OperationProcessor
 * @see OperationProcessorFactory
 * @see GdpMessage
 * @see Rtp
 */
@Component("gdpMessageProcessor")
@Slf4j
public class GdpMessageProcessor implements MessageProcessor<GdpMessage, Mono<Rtp>> {

  private final OperationProcessorFactory operationProcessorFactory;
  private final GdpEventHubProperties gdpEventHubProperties;

  /**
   * Constructs a new {@code GdpMessageProcessor} with the given {@link OperationProcessorFactory}.
   *
   * @param operationProcessorFactory the factory used to resolve operation-specific processors
   */
  public GdpMessageProcessor(
      @NonNull final OperationProcessorFactory operationProcessorFactory,
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {
    this.operationProcessorFactory = Objects.requireNonNull(operationProcessorFactory);
    this.gdpEventHubProperties = Objects.requireNonNull(gdpEventHubProperties);
  }

  /**
   * Processes the given {@link GdpMessage} by determining its {@link Operation} type
   * and delegating the processing to a corresponding {@link OperationProcessor}.
   *
   * <p>If the operation is unsupported, this method returns a {@link Mono#error}.
   * Otherwise, it returns the result of the delegated operation processor.</p>
   *
   * @param message the GDP message to process; must not be {@code null}
   * @return a {@link Mono} emitting the resulting {@link Rtp} or an error if unsupported or failed
   * @throws NullPointerException if the input message is {@code null}
   */
  @Override
  @NonNull
  public Mono<Rtp> processMessage(@NonNull final GdpMessage message) {
    Objects.requireNonNull(message, "GdpMessage cannot be null");

    return Mono.fromSupplier(() -> message)
        .doOnNext(payload -> log.info("Operation: {}", payload.operation()))
        .flatMap(payload -> this.operationProcessorFactory
                .getProcessor(payload)
                .flatMap(operationProcessor -> operationProcessor.processOperation(payload)))
        .contextWrite(ctx -> ctx
                .put("foreignStatus", Objects.requireNonNull(message.status(), "foreignStatus is required"))
                .put("eventDispatcher", Objects.requireNonNull(this.gdpEventHubProperties.eventDispatcher(), "eventDispatcher is required")));
  }
}
