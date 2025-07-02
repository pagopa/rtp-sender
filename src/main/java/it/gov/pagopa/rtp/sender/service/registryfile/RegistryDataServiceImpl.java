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


@Service("registryDataService")
@RegisterReflection(classes = {ServiceProviderFullData.class,})
@Slf4j
public class RegistryDataServiceImpl implements RegistryDataService {

  private final BlobStorageClient blobStorageClient;


  public RegistryDataServiceImpl(
      @NonNull final BlobStorageClient blobStorageClient) {

    this.blobStorageClient = Objects.requireNonNull(
        blobStorageClient, "Blob storage client cannot be null");
  }


  @Override
  @NonNull
  @Cacheable("registry-data")
  public Mono<Map<String, ServiceProviderFullData>> getRegistryData() {
    return this.getRawSRegistryData()
        .flatMap(this::transformRegistryFileData)
        .onErrorMap(ExceptionUtils::gracefullyHandleError)
        .doOnSuccess(data -> log.info("Successfully transformed registry data"))
        .doOnError(error -> log.error("Error retrieving registry data: {}", error.getMessage(), error));
  }


  @Override
  @NonNull
  @Cacheable("service-providers-by-psp-tax-code")
  public Mono<Map<String, ServiceProvider>> getServiceProvidersByPspTaxCode() {
    return this.getRawSRegistryData()
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


  @NonNull
  private Mono<ServiceProviderDataResponse> getRawSRegistryData() {
    return this.blobStorageClient.getServiceProviderData()
        .doFirst(() -> log.info("Starting getServiceProviderData"))
        .onErrorMap(ExceptionUtils::gracefullyHandleError)
        .doOnNext(rawData -> log.debug("Successfully retrieved registry raw data"))
        .doOnError(error -> log.error("Error retrieving registry data: {}", error.getMessage()));
  }


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
                              tspMap.get(sp.tspId())
                          )
                      ));
        });
  }

}
