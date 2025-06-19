package it.gov.pagopa.rtp.sender.service.rtp;

import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import reactor.core.publisher.Mono;

/**
 * Interface defining state transition operations for {@link Rtp} entities.
 * <p>
 * Each method corresponds to a specific domain event that triggers a transition
 * within a state machine. Implementations are responsible for applying the event
 * and returning the updated {@link Rtp} instance.
 */
public interface RtpStatusUpdater {

  Mono<Rtp> triggerSendRtp(Rtp rtp);

  Mono<Rtp> triggerCancelRtp(Rtp rtp);

  Mono<Rtp> triggerAcceptRtp(Rtp rtp);

  Mono<Rtp> triggerRejectRtp(Rtp rtp);

  Mono<Rtp> triggerUserAcceptRtp(Rtp rtp);

  Mono<Rtp> triggerUserRejectRtp(Rtp rtp);

  Mono<Rtp> triggerPayRtp(Rtp rtp);

  Mono<Rtp> triggerErrorSendRtp(Rtp rtp);

  Mono<Rtp> triggerErrorCancelRtp(Rtp rtp);

  Mono<Rtp> triggerCancelRtpAccr(Rtp rtp);

  Mono<Rtp> triggerCancelRtpRejected(Rtp rtp);

  Mono<Boolean> canCancel(Rtp rtp);
}
