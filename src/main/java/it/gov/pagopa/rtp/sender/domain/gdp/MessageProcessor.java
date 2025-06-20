package it.gov.pagopa.rtp.sender.domain.gdp;

/**
 * A generic interface for processing messages.
 *
 * <p>Implementations of this interface define how to transform or handle a message of type {@code IN}
 * and return a result of type {@code OUT}. This can be used for both synchronous and asynchronous processing,
 * depending on the types used (e.g., wrapping {@code OUT} in a reactive type such as {@code Mono} or {@code Flux}).</p>
 *
 * @param <I>  the type of input message to process
 * @param <O> the type of result produced after processing the message
 */
public interface MessageProcessor<I, O> {

  /**
   * Processes the given input message and returns a result.
   *
   * @param message the message to process
   * @return the result of processing the message
   */
  O processMessage(I message);

}

