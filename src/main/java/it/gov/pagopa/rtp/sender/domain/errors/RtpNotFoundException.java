package it.gov.pagopa.rtp.sender.domain.errors;

import java.util.UUID;

public class RtpNotFoundException extends RuntimeException {

  private static final String DEFAULT_ERROR_MESSAGE = "Rtp not found. Id: %s";
  private static final String COMPOSITE_KEY_ERROR_MESSAGE = "RTP not found by composite key: OperationId %s and EventDispatcher %s";


  public RtpNotFoundException(String message) {
    super(message);
  }

  public RtpNotFoundException(UUID id) {
    super(String.format(DEFAULT_ERROR_MESSAGE, id));
  }

  public RtpNotFoundException(Long operationId, String eventDispatcher) {
    super(String.format(COMPOSITE_KEY_ERROR_MESSAGE, operationId, eventDispatcher));
  }
}
