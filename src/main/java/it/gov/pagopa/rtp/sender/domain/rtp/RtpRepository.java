package it.gov.pagopa.rtp.sender.domain.rtp;

import reactor.core.publisher.Mono;

/**
 * Repository interface for accessing and persisting {@link Rtp} entities.
 * <p>
 * Provides reactive operations to:
 * <ul>
 *   <li>Save an RTP</li>
 *   <li>Retrieve an RTP by its resource ID</li>
 *   <li>Retrieve an RTP by operation ID and event dispatcher</li>
 *   <li>Retrieve an RTP by notice number</li>
 * </ul>
 * </p>
 *
 * @see Rtp
 */
public interface RtpRepository {

  /**
   * Persists the given {@link Rtp} domain object to the underlying data store.
   *
   * @param rtp the RTP to save
   * @return a {@link Mono} emitting the saved RTP instance
   */
  Mono<Rtp> save(Rtp rtp);

  /**
   * Retrieves an {@link Rtp} by its unique {@link ResourceID}.
   *
   * @param id the resource ID to search by
   * @return a {@link Mono} emitting the RTP if found, or empty if not found
   */
  Mono<Rtp> findById(ResourceID id);

  /**
   * Retrieves an {@link Rtp} by its operation ID and associated event dispatcher.
   *
   * @param operationId     the operation ID to search for
   * @param eventDispatcher the event dispatcher identifier
   * @return a {@link Mono} emitting the RTP if found, or empty if not found
   */
  Mono<Rtp> findByOperationIdAndEventDispatcher(Long operationId, String eventDispatcher);

  /**
   * Retrieves an {@link Rtp} by its associated notice number.
   *
   * @param noticeNumber the notice number to search for
   * @return a {@link Mono} emitting the RTP if found, or empty if not found
   */
  Mono<Rtp> findByNoticeNumber(String noticeNumber);

}
