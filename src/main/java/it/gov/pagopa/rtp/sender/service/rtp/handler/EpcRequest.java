package it.gov.pagopa.rtp.sender.service.rtp.handler;

import it.gov.pagopa.rtp.sender.domain.rtp.TransactionStatus;
import java.util.Objects;
import lombok.With;
import org.springframework.lang.NonNull;

import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProviderFullData;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;


/**
 * A record representing a request for an EPC (Electronic Payment Confirmation).
 *
 * @param rtpToSend the RTP (Request to Pay) to be sent
 * @param serviceProviderFullData the full data of the service provider
 * @param token an optional token for authentication or identification
 * @param response an optional response from a synchronous SEPA request to pay creation
 */
@With
public record EpcRequest(
    Rtp rtpToSend,
    ServiceProviderFullData serviceProviderFullData,
    String token,
    TransactionStatus response
) {

  /**
   * Creates an instance of {@link EpcRequest} with the specified RTP to send.
   *
   * @param rtpToSend the RTP to send; must not be null
   * @return a new instance of {@link EpcRequest} with the specified RTP and null for other fields
   * @throws NullPointerException if the provided rtpToSend is null
   */
  public static EpcRequest of(@NonNull final Rtp rtpToSend) {

    Objects.requireNonNull(rtpToSend, "Rtp to send cannot be null.");

    return new EpcRequest(rtpToSend, null, null, null);
  }

}
