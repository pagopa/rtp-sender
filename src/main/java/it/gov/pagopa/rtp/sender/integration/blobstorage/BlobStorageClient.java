package it.gov.pagopa.rtp.sender.integration.blobstorage;

import reactor.core.publisher.Mono;

/*
 * Used to access to blob storage account.
 */
public interface BlobStorageClient {
    Mono<ServiceProviderDataResponse> getServiceProviderData();
}
