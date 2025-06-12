package it.gov.pagopa.rtp.sender.service.rtp;

import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SendRTPService {

    Mono<Rtp> send(Rtp rtp);

    Mono<Rtp> cancelRtp(ResourceID rtpId);

    Mono<Rtp> findRtp(UUID rtpId);
}
