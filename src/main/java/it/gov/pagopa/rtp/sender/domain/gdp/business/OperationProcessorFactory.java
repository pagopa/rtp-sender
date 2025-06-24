package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


/**
 * Factory for producing {@link OperationProcessor} instances based on the {@link Operation}
 * specified in a given {@link GdpMessage}.
 *
 * <p>This factory supports a mapping between GDP operations and corresponding processor
 * implementations. Currently, only {@link Operation#CREATE} is supported via the
 * {@link CreateOperationProcessor}.</p>
 *
 * @see Operation
 * @see GdpMessage
 * @see OperationProcessor
 * @see CreateOperationProcessor
 */
@Component("operationProcessorFactory")
@Slf4j
public class OperationProcessorFactory {

  private final GdpMapper gdpMapper;
  private final SendRTPService sendRTPService;


  /**
   * Constructs a new {@code OperationProcessorFactory} with the required dependencies.
   *
   * @param gdpMapper       the GDP-to-RTP mapper; must not be {@code null}
   * @param sendRTPService  the RTP sending service; must not be {@code null}
   * @throws NullPointerException if any argument is {@code null}
   */
  public OperationProcessorFactory(
      @NonNull final GdpMapper gdpMapper,
      @NonNull final SendRTPService sendRTPService) {

    this.gdpMapper = Objects.requireNonNull(gdpMapper);
    this.sendRTPService = Objects.requireNonNull(sendRTPService);
  }


  /**
   * Retrieves an appropriate {@link OperationProcessor} based on the {@link Operation} of the given {@link GdpMessage}.
   *
   * <p>If the operation is not supported, a {@link UnsupportedOperationException} is returned via {@link Mono#error}.</p>
   *
   * @param gdpMessage the GDP message from which to extract the operation; must not be {@code null}
   * @return a {@link Mono} emitting the resolved {@link OperationProcessor}
   * @throws NullPointerException if {@code gdpMessage} is {@code null}
   */
  @NonNull
  public Mono<OperationProcessor> getProcessor(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "GdpMessage cannot be null");

    return Mono.just(gdpMessage)
        .doFirst(() -> log.debug("Creating processor instance for operation {}", gdpMessage.operation()))
        .map(GdpMessage::operation)
        .mapNotNull(this::createProcessorInstance)
        .switchIfEmpty(
            Mono.error(new UnsupportedOperationException(gdpMessage.operation().toString())))
        .doOnSuccess(processor -> log.debug("Created processor instance for operation {}", gdpMessage.operation()))
        .doOnError(error -> log.error("Error creating processor instance for operation {}", gdpMessage.operation(), error));
  }


  /**
   * Internal method to instantiate the {@link OperationProcessor} for a given {@link Operation}.
   *
   * @param operation the GDP operation to process; must not be {@code null}
   * @return the corresponding {@link OperationProcessor} instance
   * @throws UnsupportedOperationException if the operation is unsupported
   * @throws NullPointerException if {@code operation} is {@code null}
   */
  @NonNull
  private OperationProcessor createProcessorInstance(@NonNull final Operation operation) {
    Objects.requireNonNull(operation, "Operation cannot be null");

    return switch (operation) {
      case CREATE -> new CreateOperationProcessor(this.gdpMapper, this.sendRTPService);
      case UPDATE -> throw new UnsupportedOperationException(operation.toString());
      case DELETE -> throw new UnsupportedOperationException(operation.toString());
    };
  }

}

