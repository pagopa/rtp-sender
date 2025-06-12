package it.gov.pagopa.rtp.sender.integration.blobstorage;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import it.gov.pagopa.rtp.sender.configuration.BlobStorageConfig;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * Configuration class for setting up Azure Blob Storage-related beans.
 */
@Configuration("azureConfig")
public class AzureConfig {

    private static final String FULL_ACCOUNT_NAME_TEMPLATE = "https://%s.blob.core.windows.net";


    /**
     * Provides a {@link BlobServiceClientBuilder} bean for building Blob Storage clients.
     *
     * @return a new instance of {@link BlobServiceClientBuilder}
     */
    @Bean
    public BlobServiceClientBuilder blobServiceClientBuilder() {
        return new BlobServiceClientBuilder();
    }


    /**
     * Provides a {@link DefaultAzureCredentialBuilder} bean used for authenticating with Azure services.
     *
     * @return a new instance of {@link DefaultAzureCredentialBuilder}
     */
    @Bean
    public DefaultAzureCredentialBuilder defaultAzureCredentialBuilder() {
        return new DefaultAzureCredentialBuilder();
    }


    /**
     * Builds and provides an asynchronous Azure {@link BlobServiceAsyncClient} bean.
     * This client is used to interact with Azure Blob Storage.
     *
     * @param blobStorageConfig configuration object containing the Azure Blob Storage account name
     * @param blobServiceClientBuilder builder for constructing the Blob service client
     * @param defaultAzureCredentialBuilder builder for creating the Azure credential
     * @return an instance of {@link BlobServiceAsyncClient}
     * @throws IllegalArgumentException if the storage account name is missing or invalid
     */
    @Bean("blobServiceAsyncClient")
    @NonNull
    public BlobServiceAsyncClient blobServiceAsyncClient(
        @NonNull final BlobStorageConfig blobStorageConfig,
        @NonNull final BlobServiceClientBuilder blobServiceClientBuilder,
        @NonNull final DefaultAzureCredentialBuilder defaultAzureCredentialBuilder) {

        final var endpoint = Optional.of(blobStorageConfig)
            .map(BlobStorageConfig::storageAccountName)
            .map(StringUtils::trimToNull)
            .map(storageAccountName ->
                String.format(FULL_ACCOUNT_NAME_TEMPLATE, storageAccountName))
            .orElseThrow(() -> new IllegalArgumentException("Couldn't create blob service client"));

        return blobServiceClientBuilder
            .endpoint(endpoint)
            .credential(defaultAzureCredentialBuilder.build())
            .buildAsyncClient();
    }
}
