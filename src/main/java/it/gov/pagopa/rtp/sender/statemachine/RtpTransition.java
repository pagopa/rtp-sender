package it.gov.pagopa.rtp.sender.statemachine;

import java.util.List;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpEntity;
import java.util.function.UnaryOperator;
import reactor.core.publisher.Mono;


public class RtpTransition extends Transition<RtpEntity, RtpStatus, RtpEvent> {


  public RtpTransition(
      RtpStatus source, RtpEvent event, RtpStatus destination,
      List<UnaryOperator<Mono<RtpEntity>>> preTransactionActions,
      List<UnaryOperator<Mono<RtpEntity>>> postTransactionActions) {

    super(source, event, destination, preTransactionActions, postTransactionActions);
  }
}
