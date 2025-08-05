package it.gov.pagopa.rtp.sender.exception;

public class GdpMessageDeserializationException extends RuntimeException {
  public GdpMessageDeserializationException(String message, Throwable cause) {
    super(message, cause);
  }
}