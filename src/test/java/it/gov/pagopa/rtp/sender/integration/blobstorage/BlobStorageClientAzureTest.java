package it.gov.pagopa.rtp.sender.integration.blobstorage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;

import it.gov.pagopa.rtp.sender.configuration.BlobStorageConfig;
import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProvider;
import it.gov.pagopa.rtp.sender.domain.registryfile.TechnicalServiceProvider;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlobStorageClientAzureTest {

    @Mock
    private BlobStorageConfig blobStorageConfig;

    @Mock
    private BlobServiceAsyncClient blobServiceClient;

    @Mock
    private BlobContainerAsyncClient blobContainerClient;

    @Mock
    private BlobAsyncClient blobClient;

    @Mock
    private BinaryData binaryData;

    private BlobStorageClientAzure blobStorageClientAzure;

    @BeforeEach
    void setUp() {
        // Mock configuration
        when(blobStorageConfig.containerName()).thenReturn("testcontainer");
        when(blobStorageConfig.blobName()).thenReturn("testblob.json");

        // Create test instance
        blobStorageClientAzure = new BlobStorageClientAzure(blobStorageConfig, blobServiceClient);
    }

    @Test
    void getServiceProviderData_Success() {
        // Prepare test data
        ServiceProviderDataResponse expectedResponse = new ServiceProviderDataResponse(
            List.of(
                new TechnicalServiceProvider("08992631005", "CBI S.c.p.a.", "https://api.cbi.it", "6A7672BD13DAEEBEA96A2D1D", null, true),
                new TechnicalServiceProvider("BPPIITRRXXX", "Poste Italiane", "https://api.poste.it", "...", null, true)
            ),
            List.of(
                new ServiceProvider("UNCRITMM", "UniCredit S.p.A.", "08992631005"),
                new ServiceProvider("BPPIITRRXXX", "Poste Italiane S.p.A.", "BPPIITRRXXX")
            )
        );
        
        // Mock the blob storage chain
        when(blobServiceClient.getBlobContainerAsyncClient(anyString())).thenReturn(blobContainerClient);
        when(blobContainerClient.getBlobAsyncClient(anyString())).thenReturn(blobClient);
        when(blobClient.downloadContent()).thenReturn(Mono.just(binaryData));
        when(binaryData.toObject(ServiceProviderDataResponse.class)).thenReturn(expectedResponse);

        // Test
        Mono<ServiceProviderDataResponse> result = blobStorageClientAzure.getServiceProviderData();

        // Verify
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        // Verify interactions
        verify(blobServiceClient).getBlobContainerAsyncClient("testcontainer");
        verify(blobContainerClient).getBlobAsyncClient("testblob.json");
        verify(blobClient).downloadContent();
        verify(binaryData).toObject(ServiceProviderDataResponse.class);
    }

    @Test
    void givenBlobDownloadError_whenGetServiceProviderData_thenErrorIsLoggedAndPropagated() {

        when(blobServiceClient.getBlobContainerAsyncClient(anyString()))
            .thenReturn(blobContainerClient);
        when(blobContainerClient.getBlobAsyncClient(anyString()))
            .thenReturn(blobClient);
        when(blobClient.downloadContent())
            .thenReturn(Mono.error(new RuntimeException("Download failed")));

        final var resultMono = blobStorageClientAzure.getServiceProviderData();

        StepVerifier.create(resultMono)
            .expectErrorMatches(error -> error instanceof RuntimeException &&
                error.getMessage().equals("Download failed"))
            .verify();
    }

}