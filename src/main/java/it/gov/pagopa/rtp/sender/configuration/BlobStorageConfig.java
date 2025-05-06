package it.gov.pagopa.rtp.sender.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blob-storage")
public record BlobStorageConfig (String storageAccountName, String containerName, String blobName, String managedIdentity){
    
}