package it.gov.pagopa.rtp.sender.integration.blobstorage;

import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.specialized.BlobAsyncClientBase;
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


/**
 * Azure-based implementation of {@link BlobStorageClient} for interacting with Azure Blob Storage using the Azure SDK.
 * <p>
 * This component is responsible for downloading and deserializing JSON blob content into Java objects
 * defined in the domain (e.g., {@link ServiceProviderDataResponse}).
 * </p>
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


  /**
   * Constructs a new {@link BlobStorageClientAzure} instance.
   *
   * @param blobStorageConfig      the configuration for accessing the Blob Storage (container and blob name)
   * @param blobServiceClient      the asynchronous Azure Blob Storage client
   * @throws NullPointerException if any argument is {@code null}
   */
  public BlobStorageClientAzure(
      @NonNull final BlobStorageConfig blobStorageConfig,
      @NonNull final BlobServiceAsyncClient blobServiceClient) {

    this.blobStorageConfig = Objects.requireNonNull(blobStorageConfig);
    this.blobServiceClient = Objects.requireNonNull(blobServiceClient);
  }


  /**
   * Retrieves and deserializes the blob content from Azure Blob Storage into a {@link ServiceProviderDataResponse}.
   * <p>
   * This method performs the following:
   * <ul>
   *   <li>Retrieves the target container and blob client based on configuration</li>
   *   <li>Downloads the blob content as binary data</li>
   *   <li>Converts the binary data to a {@link ServiceProviderDataResponse} object</li>
   * </ul>
   * </p>
   *
   * @return a {@link Mono} emitting the deserialized {@link ServiceProviderDataResponse}
   */
  @Override
  public Mono<ServiceProviderDataResponse> getServiceProviderData() {

    return Mono.just(blobServiceClient)
        .doFirst(() -> log.info("Starting getServiceProviderData for container: {} blob: {}",
            blobStorageConfig.containerName(),
            blobStorageConfig.blobName()))
        .map(serviceClient ->
            serviceClient.getBlobContainerAsyncClient(blobStorageConfig.containerName()))
        .map(containerClient ->
            containerClient.getBlobAsyncClient(blobStorageConfig.blobName()))
        .flatMap(BlobAsyncClientBase::downloadContent)
        .map(binaryData ->
            binaryData.toObject(ServiceProviderDataResponse.class))
        .doOnSuccess(data -> log.info("Successfully retrieved blob data"))
        .doOnError(error -> log.error("Error downloading blob: {}", error.getMessage(), error));
  }

}