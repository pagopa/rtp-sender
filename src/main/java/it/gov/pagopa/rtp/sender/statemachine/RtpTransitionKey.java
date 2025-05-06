package it.gov.pagopa.rtp.sender.statemachine;

import org.springframework.validation.annotation.Validated;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;

@Validated
public class RtpTransitionKey extends TransitionKey<RtpStatus, RtpEvent> {

  public RtpTransitionKey(RtpStatus source, RtpEvent event) {
    super(source, event);
  }
}
