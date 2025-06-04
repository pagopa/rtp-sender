package it.gov.pagopa.rtp.sender.domain.gdp;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import com.azure.spring.messaging.eventhubs.support.EventHubsHeaders;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Configuration("gdpEventHandler")
@RegisterReflectionForBinding(GdpMessage.class)
@Slf4j
public class GdpEventHandler {

  @Bean("gdpMessageConsumer")
  @NonNull
  public Function<Flux<Message<GdpMessage>>, Mono<Void>> gdpMessageConsumer() {
    return gdpMessage -> gdpMessage
        .doOnNext(message -> log.info(
            "New GDP message received: '{}', partition key: {}, sequence number: {}, offset: {}, enqueued time: {}",
            message.getPayload(),
            message.getHeaders().get(EventHubsHeaders.PARTITION_KEY),
            message.getHeaders().get(EventHubsHeaders.SEQUENCE_NUMBER),
            message.getHeaders().get(EventHubsHeaders.OFFSET),
            message.getHeaders().get(EventHubsHeaders.ENQUEUED_TIME)
        ))
        .doOnNext(message -> log.info("Payload: {}", message.getPayload()))

        .map(Message::getHeaders)
        .mapNotNull(headers -> headers.get(AzureHeaders.CHECKPOINTER))
        .map(Checkpointer.class::cast)
        .flatMap(Checkpointer::success)

        .doOnEach(success -> log.info("Message successfully checkpointed"))
        .doOnError(error -> log.error("Exception found", error))
        .then();
  }

}
