package it.gov.pagopa.rtp.sender.service.rtp;

import java.util.UUID;
import java.util.function.Function;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpEntity;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpMapper;
import it.gov.pagopa.rtp.sender.statemachine.StateMachine;
import it.gov.pagopa.rtp.sender.statemachine.StateMachineFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

class RtpStatusUpdaterImplTest {

  @Mock
  private StateMachineFactory<RtpEntity, RtpEvent> stateMachineFactory;

  @Mock
  private StateMachine<RtpEntity, RtpEvent> stateMachine;

  @Mock
  private RtpMapper rtpMapper;

  @Mock
  private Rtp rtp;

  @Mock
  private RtpEntity rtpEntity;

  @InjectMocks
  private RtpStatusUpdaterImpl rtpStatusUpdater;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    rtp = mock(Rtp.class);
    when(rtpMapper.toDbEntity(rtp))
        .thenReturn(rtpEntity);

    rtpStatusUpdater = new RtpStatusUpdaterImpl(() -> stateMachine, rtpMapper);
  }

  private void verifyTransition(RtpEvent event, Function<RtpStatusUpdaterImpl, Mono<Rtp>> triggerMethod) {
    final var expectedRtp = mock(Rtp.class);

    when(stateMachine.transition(rtpEntity, event))
        .thenReturn(Mono.just(rtpEntity));
    when(rtpMapper.toDomain(rtpEntity))
        .thenReturn(expectedRtp);

    StepVerifier.create(triggerMethod.apply(rtpStatusUpdater))
        .expectNext(expectedRtp)
        .verifyComplete();
  }

  private void verifyErrorPropagation(RtpEvent event, Function<RtpStatusUpdaterImpl, Mono<Rtp>> triggerMethod) {
    when(stateMachine.transition(rtpEntity, event))
        .thenReturn(Mono.error(new IllegalStateException("Transition failed")));

    StepVerifier.create(triggerMethod.apply(rtpStatusUpdater))
        .expectErrorSatisfies(error -> assertInstanceOf(IllegalStateException.class, error))
        .verify();
  }

  @Test
  void givenStateAllowsTransition_whenCanTriggerEvent_thenReturnTrue() {
    UUID fakeId = UUID.randomUUID();
    when(rtp.resourceID()).thenReturn(new ResourceID(fakeId));
    when(rtp.status()).thenReturn(RtpStatus.CREATED);
    when(stateMachine.canTransition(rtpEntity, RtpEvent.CANCEL_RTP)).thenReturn(Mono.just(true));

    StepVerifier.create(rtpStatusUpdater.canTriggerEvent(rtp, RtpEvent.CANCEL_RTP))
            .expectNext(true)
            .verifyComplete();

    verify(rtpMapper).toDbEntity(rtp);
    verify(stateMachine).canTransition(rtpEntity, RtpEvent.CANCEL_RTP);
  }


  @Test
  void givenStatePreventsTransition_whenCanTriggerEvent_thenReturnFalse() {
    UUID fakeId = UUID.randomUUID();
    when(rtp.resourceID()).thenReturn(new ResourceID(fakeId));
    when(stateMachine.canTransition(rtpEntity, RtpEvent.CANCEL_RTP)).thenReturn(Mono.just(false));

    StepVerifier.create(rtpStatusUpdater.canTriggerEvent(rtp, RtpEvent.CANCEL_RTP))
            .expectNext(false)
            .verifyComplete();

    verify(rtpMapper).toDbEntity(rtp);
    verify(stateMachine).canTransition(rtpEntity, RtpEvent.CANCEL_RTP);
  }

  @Test
  void givenStateMachineFails_whenCanTriggerEvent_thenPropagateError() {
    UUID fakeId = UUID.randomUUID();
    when(rtp.resourceID()).thenReturn(new ResourceID(fakeId));
    when(stateMachine.canTransition(rtpEntity, RtpEvent.CANCEL_RTP))
            .thenReturn(Mono.error(new RuntimeException("Unexpected failure")));

    StepVerifier.create(rtpStatusUpdater.canTriggerEvent(rtp, RtpEvent.CANCEL_RTP))
            .expectErrorMatches(err -> err instanceof RuntimeException &&
                    err.getMessage().equals("Unexpected failure"))
            .verify();

    verify(rtpMapper).toDbEntity(rtp);
    verify(stateMachine).canTransition(rtpEntity, RtpEvent.CANCEL_RTP);
  }


  @Test
  void givenValidInput_whenTriggerSendRtp_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.SEND_RTP, updater -> updater.triggerSendRtp(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerSendRtp_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.SEND_RTP, updater -> updater.triggerSendRtp(rtp));
  }

  @Test
  void givenValidInput_whenTriggerCancelRtp_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.CANCEL_RTP, updater -> updater.triggerCancelRtp(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerCancelRtp_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.CANCEL_RTP, updater -> updater.triggerCancelRtp(rtp));
  }

  @Test
  void givenValidInput_whenTriggerAcceptRtp_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.ACCEPT_RTP, updater -> updater.triggerAcceptRtp(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerAcceptRtp_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.ACCEPT_RTP, updater -> updater.triggerAcceptRtp(rtp));
  }

  @Test
  void givenValidInput_whenTriggerRejectRtp_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.REJECT_RTP, updater -> updater.triggerRejectRtp(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerRejectRtp_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.REJECT_RTP, updater -> updater.triggerRejectRtp(rtp));
  }

  @Test
  void givenValidInput_whenTriggerUserAcceptRtp_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.USER_ACCEPT_RTP, updater -> updater.triggerUserAcceptRtp(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerUserAcceptRtp_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.USER_ACCEPT_RTP, updater -> updater.triggerUserAcceptRtp(rtp));
  }

  @Test
  void givenValidInput_whenTriggerUserRejectRtp_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.USER_REJECT_RTP, updater -> updater.triggerUserRejectRtp(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerUserRejectRtp_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.USER_REJECT_RTP, updater -> updater.triggerUserRejectRtp(rtp));
  }

  @Test
  void givenValidInput_whenTriggerPayRtp_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.PAY_RTP, updater -> updater.triggerPayRtp(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerPayRtp_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.PAY_RTP, updater -> updater.triggerPayRtp(rtp));
  }

  @Test
  void givenValidInput_whenTriggerErrorSendRtp_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.ERROR_SEND_RTP, updater -> updater.triggerErrorSendRtp(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerErrorSendRtp_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.ERROR_SEND_RTP, updater -> updater.triggerErrorSendRtp(rtp));
  }

  @Test
  void givenValidInput_whenTriggerErrorCancelRtp_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.ERROR_CANCEL_RTP, updater -> updater.triggerErrorCancelRtp(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerErrorCancelRtp_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.ERROR_CANCEL_RTP, updater -> updater.triggerErrorCancelRtp(rtp));
  }

  @Test
  void givenValidInput_whenTriggerCancelRtpAccr_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.CANCEL_RTP_ACCR, updater -> updater.triggerCancelRtpAccr(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerCancelRtpAccr_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.CANCEL_RTP_ACCR, updater -> updater.triggerCancelRtpAccr(rtp));
  }

  @Test
  void givenValidInput_whenTriggerCancelRtpRejected_thenReturnUpdatedRtp() {
    verifyTransition(RtpEvent.CANCEL_RTP_REJECTED, updater -> updater.triggerCancelRtpRejected(rtp));
  }

  @Test
  void givenStateMachineFails_whenTriggerCancelRtpRejected_thenPropagateError() {
    verifyErrorPropagation(RtpEvent.CANCEL_RTP_REJECTED, updater -> updater.triggerCancelRtpRejected(rtp));
  }
}
