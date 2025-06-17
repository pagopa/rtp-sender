package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import reactor.core.publisher.Mono;

public interface OperationProcessor {

  Mono<Rtp> processOperation(GdpMessage gdpMessage);

}
