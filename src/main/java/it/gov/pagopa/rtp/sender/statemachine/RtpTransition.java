package it.gov.pagopa.rtp.sender.statemachine;

import java.util.List;
import java.util.function.Consumer;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpEntity;


public class RtpTransition extends Transition<RtpEntity, RtpStatus, RtpEvent> {


  public RtpTransition(
      RtpStatus source, RtpEvent event, RtpStatus destination,
      List<Consumer<RtpEntity>> preTransactionActions,
      List<Consumer<RtpEntity>> postTransactionActions) {

    super(source, event, destination, preTransactionActions, postTransactionActions);
  }
}
