package it.gov.pagopa.rtp.sender.integration.blobstorage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;

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
}
