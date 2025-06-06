package it.gov.pagopa.rtp.sender.domain.gdp;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.storage.blob.BlobServiceAsyncClient;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;


@Configuration("eventHubConfig")
public class EventHubConfig {


  @Bean("eventHubClientBuilder")
  @NonNull
  public EventHubClientBuilder eventHubClientBuilder() {
    return new EventHubClientBuilder();
  }



  @Bean("eventHubConsumerAsyncClient")
  @NonNull
  public EventHubConsumerAsyncClient eventHubConsumerAsyncClient(
      @NonNull final EventHubClientBuilder eventHubClientBuilder,
      @NonNull @Value("${spring.cloud.azure.eventhubs.connection-string}") final String eventHubConnectionString,
      @NonNull @Value("${spring.cloud.azure.eventhubs.event-hub-name}") final String eventHubName
  ) {
    return eventHubClientBuilder
        .connectionString(eventHubConnectionString)
        .eventHubName(eventHubName)
        .consumerGroup("$Default")
        .buildAsyncConsumerClient();
  }


  @Bean("checkpointStore")
  @NonNull
  public CheckpointStore checkpointStore(
      @NonNull final BlobServiceAsyncClient blobServiceAsyncClient) {

    return Optional.of(blobServiceAsyncClient)
        .map(serviceClient ->
            serviceClient.getBlobContainerAsyncClient("gdp-message-checkpoints")) //TODO: move to config
        .map(BlobCheckpointStore::new)
        .orElseThrow(() -> new IllegalStateException("Couldn't create checkpoint store"));
  }

}
