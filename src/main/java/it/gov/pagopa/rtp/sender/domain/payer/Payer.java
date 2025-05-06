package it.gov.pagopa.rtp.sender.domain.payer;

import java.time.Instant;

public record Payer(ActivationID activationID, String serviceProviderDebtor, String fiscalCode, Instant effectiveActivationDate)  {
}
