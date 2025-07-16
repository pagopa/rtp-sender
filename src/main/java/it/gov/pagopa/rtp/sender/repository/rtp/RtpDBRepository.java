package it.gov.pagopa.rtp.sender.repository.rtp;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpRepository;
import reactor.core.publisher.Mono;

/**
 * Implementation of the {@link RtpRepository} interface, responsible for
 * performing CRUD operations on {@link RtpEntity} via the underlying {@link RtpDB} database layer.
 * <p>
 * This repository handles domain-to-entity mapping using {@link RtpMapper} and provides
 * operations for saving and retrieving {@link Rtp} instances by various identifiers, such as resource ID,
 * notice number, and composite keys (operation ID + event dispatcher).
 * </p>
 *
 * @see Rtp
 * @see RtpEntity
 * @see RtpDB
 * @see RtpMapper
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RtpDBRepository implements RtpRepository {

  private final RtpDB rtpDB;
  private final RtpMapper rtpMapper;

  /**
   * Persists the given RTP domain object to the database.
   *
   * @param rtp the RTP domain object to save; must not be {@code null}
   * @return a {@link Mono} emitting the saved RTP domain object
   */
  @Override
  public Mono<Rtp> save(Rtp rtp) {
    log.info("Saving RTP {} in state {}", rtp.resourceID().getId(), rtp.status());
    return rtpDB.save(rtpMapper.toDbEntity(rtp))
        .map(rtpMapper::toDomain);
  }


  /**
   * Retrieves an RTP by its {@link ResourceID}.
   *
   * @param resourceID the resource ID of the RTP; must not be {@code null}
   * @return a {@link Mono} emitting the corresponding RTP if found, or an empty Mono otherwise
   */
  @NonNull
  @Override
  public Mono<Rtp> findById(@NonNull final ResourceID resourceID) {
    return Mono.just(resourceID)
        .doFirst(() -> log.debug("Retrieving RTP with id {}", resourceID.getId()))
        .map(ResourceID::getId)
        .flatMap(rtpDB::findById)
        .map(rtpMapper::toDomain);
  }

  /**
   * Retrieves an RTP using a composite key consisting of operation ID and event dispatcher.
   *
   * @param operationId     the operation ID; must not be {@code null}
   * @param eventDispatcher the event dispatcher ID; must not be {@code null}
   * @return a {@link Mono} emitting the corresponding RTP if found, or an empty Mono otherwise
   */
  @NonNull
  @Override
  public Mono<Rtp> findByOperationIdAndEventDispatcher(
          @NonNull final Long operationId,
          @NonNull final String eventDispatcher) {

    return rtpDB.findByOperationIdAndEventDispatcher(operationId,eventDispatcher)
            .doFirst(()->log.info("Retrieving RTP with operationId {} and eventDispatcher {}", operationId, eventDispatcher))
            .doOnNext(entity -> log.debug("Found RTP with operationId {} and eventDispatcher {}",
                    operationId, eventDispatcher))
            .map(rtpMapper::toDomain)
            .doOnNext(rtp -> log.debug("Mapped RTP entity to domain object: {}", rtp))
            .doOnError(error -> log.error("Error while retrieving RTP: {}", error.getMessage(), error));
  }


  /**
   * Retrieves an RTP using the given notice number.
   *
   * @param noticeNumber the notice number of the RTP; must not be {@code null}
   * @return a {@link Mono} emitting the corresponding RTP if found, or an empty Mono otherwise
   */
  @Override
  @NonNull
  public Mono<Rtp> findByNoticeNumber(@NonNull final String noticeNumber) {
    return Mono.just(noticeNumber)
        .doFirst(() -> MDC.put("notice_number", noticeNumber))

        .doFirst(() -> log.debug("Retrieving RTP by Notice Number"))
        .flatMap(rtpDB::findByNoticeNumber)
        .doOnNext(entity -> MDC.put("resource_id", entity.getResourceID().toString()))
        .doOnNext(entity -> log.debug("Found RTP by Notice Number"))

        .doOnNext(rtp -> log.debug("Mapping RTP entity to domain object"))
        .map(rtpMapper::toDomain)
        .doOnNext(rtp -> log.debug("Mapped RTP entity to domain object"))

        .doOnSuccess(entity -> Optional.ofNullable(entity)
            .ifPresentOrElse(
                rtp -> log.debug("Successfully retrieved RTP by Notice Number"),
                () -> log.warn("RTP not found by Notice Number")))
        .doOnError(error -> log.error("Error while retrieving RTP: {}", error.getMessage(), error))

        .doFinally(signal -> MDC.clear());
  }
}
