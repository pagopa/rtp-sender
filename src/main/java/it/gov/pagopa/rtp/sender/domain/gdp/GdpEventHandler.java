package it.gov.pagopa.rtp.sender.domain.gdp;

import static com.azure.spring.messaging.AzureHeaders.CHECKPOINTER;

import com.azure.spring.messaging.checkpoint.Checkpointer;
import com.azure.spring.messaging.eventhubs.support.EventHubsHeaders;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;


@Configuration("gdpEventHandler")
@RegisterReflectionForBinding(GdpMessage.class)
@Slf4j
public class GdpEventHandler {

  @Bean("gdpMessageConsumer")
  @NonNull
  public Consumer<Message<GdpMessage>> gdpMessageConsumer() {
    return gdpMessage -> {

      final var checkpointer = Optional.of(gdpMessage)
          .map(Message::getHeaders)
          .map(headers-> headers.get(CHECKPOINTER))
          .map(Checkpointer.class::cast)
          .orElseThrow(() -> new IllegalStateException("Checkpointer not found"));

      log.info("New GDP message received: '{}', partition key: {}, sequence number: {}, offset: {}, enqueued time: {}",
          gdpMessage.getPayload(),
          gdpMessage.getHeaders().get(EventHubsHeaders.PARTITION_KEY),
          gdpMessage.getHeaders().get(EventHubsHeaders.SEQUENCE_NUMBER),
          gdpMessage.getHeaders().get(EventHubsHeaders.OFFSET),
          gdpMessage.getHeaders().get(EventHubsHeaders.ENQUEUED_TIME)
      );

      checkpointer.success()
          .doOnSuccess(success-> log.info("Message successfully checkpointed: {}",
              gdpMessage.getPayload()))
          .doOnError(error-> log.error("Exception found", error))
          .block();
    };
  }

}
