package it.gov.pagopa.rtp.sender.service.registryfile;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProvider;
import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProviderFullData;
import it.gov.pagopa.rtp.sender.domain.registryfile.TechnicalServiceProvider;
import it.gov.pagopa.rtp.sender.integration.blobstorage.BlobStorageClient;
import it.gov.pagopa.rtp.sender.integration.blobstorage.ServiceProviderDataResponse;
import it.gov.pagopa.rtp.sender.utils.ExceptionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Implementation of {@link RegistryDataService} responsible for retrieving and transforming
 * registry data for service providers and their associated technical service providers.
 * <p>
 * This service loads raw registry data from Azure Blob Storage, processes it into structured
 * formats, and caches the results to improve performance.
 * </p>
 *
 * <p>
 * Uses Spring Cache abstraction to cache results:
 * <ul>
 *   <li><code>registry-data</code>: full registry mapping</li>
 *   <li><code>service-providers-by-psp-tax-code</code>: service providers indexed by PSP tax code</li>
 * </ul>
 * </p>
 */
@Service("registryDataService")
@RegisterReflection(classes = {ServiceProviderFullData.class,})
@Slf4j
public class RegistryDataServiceImpl implements RegistryDataService {

  private final BlobStorageClient blobStorageClient;


  /**
   * Constructs the service with the given blob storage client.
   *
   * @param blobStorageClient the client used to fetch raw registry data from Azure Blob Storage.
   *                          Must not be null.
   */
  public RegistryDataServiceImpl(
      @NonNull final BlobStorageClient blobStorageClient) {

    this.blobStorageClient = Objects.requireNonNull(
        blobStorageClient, "Blob storage client cannot be null");
  }


  /**
   * Retrieves and transforms the full registry data, mapping each {@link ServiceProvider} ID
   * to a {@link ServiceProviderFullData} object enriched with its corresponding
   * {@link TechnicalServiceProvider}.
   * <p>
   * Result is cached under the key {@code registry-data}.
   * </p>
   *
   * @return a {@link Mono} emitting a map of service provider ID to full registry data.
   */
  @Override
  @NonNull
  @Cacheable("registry-data")
  public Mono<Map<String, ServiceProviderFullData>> getRegistryData() {
    return this.getRawRegistryData()
        .flatMap(this::transformRegistryFileData)
        .onErrorMap(ExceptionUtils::gracefullyHandleError)
        .doOnSuccess(data -> log.info("Successfully transformed registry data"))
        .doOnError(error -> log.error("Error retrieving registry data: {}", error.getMessage(), error));
  }


  /**
   * Retrieves the service providers from the registry data and maps them by PSP tax code.
   * <p>
   * Result is cached under the key {@code service-providers-by-psp-tax-code}.
   * </p>
   *
   * @return a {@link Mono} emitting a map of PSP tax code to {@link ServiceProvider}.
   */
  @Override
  @NonNull
  @Cacheable("service-providers-by-psp-tax-code")
  public Mono<Map<String, ServiceProvider>> getServiceProvidersByPspTaxCode() {
    return this.getRawRegistryData()
        .doFirst(() -> log.debug("Retrieving service provider data map by PSP tax code"))

        .map(ServiceProviderDataResponse::sps)
        .flatMapMany(Flux::fromIterable)
        .collectMap(ServiceProvider::pspTaxCode, Function.identity())

        .onErrorMap(ExceptionUtils::gracefullyHandleError)
        .doOnSuccess(serviceProvidersMap ->
            log.info("Successfully retrieved service provider data map by PSP tax code"))
        .doOnError(error ->
            log.error("Error retrieving service provider data: {}", error.getMessage(), error));
  }


  /**
   * Fetches the raw registry data from Azure Blob Storage.
   *
   * @return a {@link Mono} emitting the raw {@link ServiceProviderDataResponse}.
   */
  @NonNull
  private Mono<ServiceProviderDataResponse> getRawRegistryData() {
    return this.blobStorageClient.getServiceProviderData()
        .doFirst(() -> log.info("Starting getServiceProviderData"))
        .onErrorMap(ExceptionUtils::gracefullyHandleError)
        .doOnNext(rawData -> log.debug("Successfully retrieved registry raw data"))
        .doOnError(error -> log.error("Error retrieving registry data: {}", error.getMessage()));
  }


  /**
   * Transforms the raw registry data by mapping each service provider ID to a full data object
   * that includes both the service provider and its corresponding technical service provider.
   *
   * @param serviceProviderDataResponse the raw registry data
   * @return a {@link Mono} emitting a map of service provider ID to {@link ServiceProviderFullData}
   */
  @NonNull
  private Mono<Map<String, ServiceProviderFullData>> transformRegistryFileData(
      @NonNull final ServiceProviderDataResponse serviceProviderDataResponse) {

    return Mono.just(serviceProviderDataResponse)
        .doOnNext(rawData -> log.debug("Transforming registry data"))
        .flatMap(data -> {
          final var technicalServiceProviderMap = Flux.fromIterable(data.tsps())
              .collectMap(TechnicalServiceProvider::id);

          return technicalServiceProviderMap.flatMap(
              tspMap ->
                  Flux.fromIterable(data.sps())
                      .collectMap(
                          ServiceProvider::id,
                          sp -> new ServiceProviderFullData(
                              sp.id(),
                              sp.name(),
                              sp.pspTaxCode(),
                              tspMap.get(sp.tspId())
                          )
                      ));
        });
  }

}
