package it.gov.pagopa.rtp.sender.domain.rtp;

/**
 * Enumeration of possible transaction statuses for Request-To-Pay (RTP) flows.
 */
public enum TransactionStatus {

  ACTC,
  ACCP,
  RJCT,
  ERROR,
  CNCL,
  RJCR,
  ACWC

}
