package it.gov.pagopa.rtp.sender.domain.errors;

public class SepaRequestException extends RuntimeException {

  public SepaRequestException(String message) {
    super(message);
  }
}
