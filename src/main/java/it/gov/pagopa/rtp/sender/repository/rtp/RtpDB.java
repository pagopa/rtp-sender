package it.gov.pagopa.rtp.sender.repository.rtp;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import it.gov.pagopa.rtp.sender.telemetry.TraceMongo;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@TraceMongo
public interface RtpDB extends ReactiveMongoRepository<RtpEntity, UUID> {

    Mono<RtpEntity> findByOperationIdAndEventDispatcher(Long operationId, String eventDispatcher);
}
