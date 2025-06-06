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

@Configuration
public class AzureConfig {
    
    @Bean
    public BlobServiceClientBuilder blobServiceClientBuilder() {
        return new BlobServiceClientBuilder();
    }
    
    @Bean
    public DefaultAzureCredentialBuilder defaultAzureCredentialBuilder() {
        return new DefaultAzureCredentialBuilder();
    }


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
                String.format("https://%s.blob.core.windows.net", storageAccountName))
            .orElseThrow(() -> new IllegalArgumentException("Couldn't create blob service client"));

        return blobServiceClientBuilder
            .endpoint(endpoint)
            .credential(defaultAzureCredentialBuilder.build())
            .buildAsyncClient();
    }
}
