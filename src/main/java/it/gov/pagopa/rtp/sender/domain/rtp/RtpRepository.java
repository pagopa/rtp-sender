package it.gov.pagopa.rtp.sender.domain.rtp;

import reactor.core.publisher.Mono;

public interface RtpRepository {

  Mono<Rtp> save(Rtp rtp);
  Mono<Rtp> findById(ResourceID id);

}
