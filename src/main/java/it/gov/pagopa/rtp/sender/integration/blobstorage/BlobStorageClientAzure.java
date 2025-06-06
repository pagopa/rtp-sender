package it.gov.pagopa.rtp.sender.integration.blobstorage;

import com.azure.storage.blob.BlobServiceAsyncClient;
import it.gov.pagopa.rtp.sender.configuration.BlobStorageConfig;
import it.gov.pagopa.rtp.sender.domain.registryfile.OAuth2;
import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProvider;
import it.gov.pagopa.rtp.sender.domain.registryfile.TechnicalServiceProvider;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
  private final BlobServiceAsyncClient blobServiceClient;

  public BlobStorageClientAzure(
      @NonNull final BlobStorageConfig blobStorageConfig,
      @NonNull final BlobServiceAsyncClient blobServiceClient) {

    this.blobStorageConfig = Objects.requireNonNull(blobStorageConfig);
    this.blobServiceClient = Objects.requireNonNull(blobServiceClient);
  }

  @Override
  public Mono<ServiceProviderDataResponse> getServiceProviderData() {
    log.info("Starting getServiceProviderData for container: {} blob: {}",
        blobStorageConfig.containerName(),
        blobStorageConfig.blobName());

    final var containerClient = blobServiceClient
        .getBlobContainerAsyncClient(blobStorageConfig.containerName());

    final var blobClient = containerClient
        .getBlobAsyncClient(blobStorageConfig.blobName());

    return blobClient.downloadContent()
        .map(binaryData -> binaryData.toObject(ServiceProviderDataResponse.class))
        .doOnError(error -> log.error("Error downloading blob: {}", error.getMessage()))
        .doOnSuccess(data -> log.info("Successfully retrieved blob data"));
  }

}