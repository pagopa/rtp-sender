package it.gov.pagopa.rtp.sender.domain.rtp;

public enum RtpEvent {
  CREATE_RTP,
  SEND_RTP,
  CANCEL_RTP,
  ACCEPT_RTP,
  REJECT_RTP,
  USER_ACCEPT_RTP,
  USER_REJECT_RTP,
  PAY_RTP,
  ERROR_SEND_RTP,
  ERROR_CANCEL_RTP,
  CANCEL_RTP_ACCR,
  CANCEL_RTP_REJECTED
}
