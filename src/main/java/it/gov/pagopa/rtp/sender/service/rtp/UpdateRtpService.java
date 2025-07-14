package it.gov.pagopa.rtp.sender.service.rtp;

import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import reactor.core.publisher.Mono;

public interface UpdateRtpService {

  Mono<Rtp> updateRtpPaid(Rtp rtp);
  Mono<Rtp> updateRtpCancelPaid(Rtp rtp);

}
