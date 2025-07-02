package it.gov.pagopa.rtp.sender.domain.rtp;

public enum RtpStatus {
  CREATED,
  SENT,
  CANCELLED,
  ACCEPTED,
  REJECTED,
  USER_ACCEPTED,
  USER_REJECTED,
  PAID,
  ERROR_SEND,
  CANCELLED_ACCR,
  CANCELLED_REJECTED,
  ERROR_CANCEL
}
