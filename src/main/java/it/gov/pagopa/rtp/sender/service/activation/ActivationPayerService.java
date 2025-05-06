package it.gov.pagopa.rtp.sender.service.activation;

import it.gov.pagopa.rtp.sender.domain.payer.Payer;
import reactor.core.publisher.Mono;

public interface ActivationPayerService {
   Mono<Payer> activatePayer(String payer, String fiscalCode);
   Mono<Payer> findPayer(String payer);
}