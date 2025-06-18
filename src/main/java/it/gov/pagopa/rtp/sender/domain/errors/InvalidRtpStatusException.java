package it.gov.pagopa.rtp.sender.domain.errors;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidRtpStatusException extends RuntimeException {

  public InvalidRtpStatusException(UUID rtpId, RtpStatus status) {
    super(String.format("Cannot transition RTP with id %s from status %s",
            rtpId, status));
  }
}
