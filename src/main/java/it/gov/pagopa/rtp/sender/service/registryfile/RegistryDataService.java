package it.gov.pagopa.rtp.sender.service.registryfile;

import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProvider;
import java.util.Map;

import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProviderFullData;
import reactor.core.publisher.Mono;

/**
 * Service interface for accessing registry data related to service providers.
 * <p>
 * Provides methods to retrieve enriched registry information and simplified views of
 * service providers based on PSP tax codes.
 * </p>
 */
public interface RegistryDataService {

  /**
   * Retrieves the full registry data as a mapping from service provider IDs to enriched
   * service provider information.
   *
   * @return a {@link Mono} emitting a map where keys are service provider IDs and values
   *         are {@link ServiceProviderFullData} instances containing enriched registry data
   */
  Mono<Map<String, ServiceProviderFullData>> getRegistryData();

  /**
   * Retrieves service providers mapped by their PSP tax code.
   *
   * @return a {@link Mono} emitting a map where keys are PSP tax codes and values
   *         are the corresponding {@link ServiceProvider} instances
   */
  Mono<Map<String, ServiceProvider>> getServiceProvidersByPspTaxCode();

}
