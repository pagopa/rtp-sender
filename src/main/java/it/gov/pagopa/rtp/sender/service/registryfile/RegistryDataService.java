package it.gov.pagopa.rtp.sender.service.registryfile;

import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProvider;
import java.util.Map;

import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProviderFullData;
import reactor.core.publisher.Mono;

public interface RegistryDataService {

  Mono<Map<String, ServiceProviderFullData>> getRegistryData();

  Mono<Map<String, ServiceProvider>> getServiceProvidersByPspTaxCode();

}
