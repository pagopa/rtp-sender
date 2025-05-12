package it.gov.pagopa.rtp.sender.repository.activation;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import it.gov.pagopa.rtp.sender.telemetry.TraceMongo;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@TraceMongo
public interface ActivationDB extends ReactiveMongoRepository<ActivationEntity, UUID> {
  Mono<ActivationEntity> findByFiscalCode(String fiscalCode);
}
