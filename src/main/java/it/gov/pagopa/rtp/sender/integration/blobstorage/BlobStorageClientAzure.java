package it.gov.pagopa.rtp.sender.integration.blobstorage;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import it.gov.pagopa.rtp.sender.configuration.BlobStorageConfig;
import it.gov.pagopa.rtp.sender.domain.registryfile.OAuth2;
import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProvider;
import it.gov.pagopa.rtp.sender.domain.registryfile.TechnicalServiceProvider;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import lombok.extern.slf4j.Slf4j;

/*
 * Interact with the Azure Blob Storage using the Azure SDK Library, 
 * and performing 
 */
@Component
@Slf4j
@RegisterReflectionForBinding({
    ServiceProviderDataResponse.class,
    TechnicalServiceProvider.class,
    OAuth2.class,
    ServiceProvider.class,
})
public class BlobStorageClientAzure implements BlobStorageClient {

  private final BlobStorageConfig blobStorageConfig;
  private final BlobServiceClient blobServiceClient;

  public BlobStorageClientAzure(BlobStorageConfig blobStorageConfig,
      BlobServiceClientBuilder blobServiceClientBuilder) {
    this.blobStorageConfig = blobStorageConfig;
    String endpoint = String.format("https://%s.blob.core.windows.net",
        blobStorageConfig.storageAccountName());

    this.blobServiceClient = blobServiceClientBuilder
        .endpoint(endpoint)
        .credential(new DefaultAzureCredentialBuilder().build())
        .buildClient();
  }

  @Override
  public Mono<ServiceProviderDataResponse> getServiceProviderData() {
    return Mono.fromCallable(() -> {
      log.info("Starting getServiceProviderData for container: {} blob: {}",
          blobStorageConfig.containerName(),
          blobStorageConfig.blobName());

      BlobContainerClient containerClient = blobServiceClient
          .getBlobContainerClient(blobStorageConfig.containerName());

      BlobClient blobClient = containerClient
          .getBlobClient(blobStorageConfig.blobName());

      return blobClient.downloadContent().toObject(ServiceProviderDataResponse.class);
    })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(error -> log.error("Error downloading blob: {}", error.getMessage()))
        .doOnSuccess(data -> log.info("Successfully retrieved blob data"));
  }

}