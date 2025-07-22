package it.gov.pagopa.rtp.sender.service.rtp;

import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SendRTPService {

    Mono<Rtp> send(Rtp rtp);

    Mono<Rtp> cancelRtpById(ResourceID rtpId);

    Mono<Rtp> findRtp(UUID rtpId);

    Flux<Rtp> findRtpsByNoticeNumber(String noticeNumber);

    Mono<Rtp> findRtpByCompositeKey(Long operationId, String eventDispatcher);
}
