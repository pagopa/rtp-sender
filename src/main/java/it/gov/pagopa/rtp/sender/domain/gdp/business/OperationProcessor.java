package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import reactor.core.publisher.Mono;

/**
 * Strategy interface for processing a specific {@link GdpMessage} based on its {@link Operation} type.
 *
 * <p>Implementations of this interface encapsulate logic for handling individual operation types such as
 * {@link Operation#CREATE}, {@link Operation#UPDATE}, or {@link Operation#DELETE}.</p>
 *
 * <p>The processing is expected to be asynchronous, returning a {@link Mono} containing the resulting {@link Rtp}.</p>
 *
 * @see GdpMessage
 * @see Operation
 * @see Rtp
 */
public interface OperationProcessor {

  /**
   * Processes the given {@link GdpMessage} and returns the resulting {@link Rtp} wrapped in a {@link Mono}.</p>
   *
   * @param gdpMessage the GDP message to process; must not be {@code null}
   * @return a {@link Mono} emitting the resulting {@link Rtp}
   */
  Mono<Rtp> processOperation(GdpMessage gdpMessage);

}

