package it.gov.pagopa.rtp.sender.repository.rtp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpRepository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RtpDBRepository implements RtpRepository {

  private final RtpDB rtpDB;
  private final RtpMapper rtpMapper;

  @Override
  public Mono<Rtp> save(Rtp rtp) {
    log.info("Saving RTP {} in state {}", rtp.resourceID().getId(), rtp.status());
    return rtpDB.save(rtpMapper.toDbEntity(rtp))
        .map(rtpMapper::toDomain);
  }


  @NonNull
  @Override
  public Mono<Rtp> findById(@NonNull final ResourceID resourceID) {
    return Mono.just(resourceID)
        .doFirst(() -> log.debug("Retrieving RTP with id {}", resourceID.getId()))
        .map(ResourceID::getId)
        .flatMap(rtpDB::findById)
        .map(rtpMapper::toDomain);
  }

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
}
