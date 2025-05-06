package it.gov.pagopa.rtp.sender.domain.payer;


import reactor.core.publisher.Mono;

public interface PayerRepository {
    
    // Used to check if a specific payer is already registered.
    Mono<Payer> findByFiscalCode(String fiscalCode);
    
    Mono<Payer> save(Payer payer);
    
}