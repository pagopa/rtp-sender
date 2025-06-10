package it.gov.pagopa.rtp.sender.domain.gdp;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;


@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
class GdpEventHandlerTest {

  @Value("${test.kafka.topic}")
  private String topic;

  @Autowired
  private InputDestination inputDestination;

  private GdpEventHandler gdpEventHandler;


  @BeforeEach
  void setUp() {
    gdpEventHandler = new GdpEventHandler();
  }


  @Test
  void givenValidGdpMessage_whenConsumed_thenProcessedSuccessfully() {
    final var validMessage = createValidGdpMessage(1L);
    final var message = createKafkaMessage(validMessage, 0, 123L);

    inputDestination.send(message, topic);

    StepVerifier.create(
        gdpEventHandler.gdpMessageConsumer()
            .apply(Flux.just(message)))
        .verifyComplete();
  }

  @Test
  void givenMultipleMessages_whenConsumed_thenAllProcessed() {
    final var msg1 = createValidGdpMessage(1L);
    final var msg2 = createValidGdpMessage(2L);

    final var messages = Flux.just(
        createKafkaMessage(msg1, 1, 123L),
        createKafkaMessage(msg2, 1, 124L)
    );

    StepVerifier.create(
        gdpEventHandler.gdpMessageConsumer()
            .apply(messages))
        .verifyComplete();
  }


  private GdpMessage createValidGdpMessage(final long id) {
    return GdpMessage.builder()
        .id(id)
        .operation(GdpMessage.Operation.CREATE)
        .timestamp(System.currentTimeMillis())
        .iuv("testIuv")
        .subject("testSubject")
        .description("testDescription")
        .ecTaxCode("testEcTaxCode")
        .debtorTaxCode("testDebtorTaxCode")
        .nav("testNav")
        .dueDate(LocalDate.now())
        .amount(100)
        .status(GdpMessage.Status.VALID)
        .pspCode("testPspCode")
        .pspTaxCode("testPspTaxCode")
        .build();
  }

  private Message<GdpMessage> createKafkaMessage(GdpMessage payload, int partition, long offset) {
    return MessageBuilder.withPayload(payload)
        .setHeader(KafkaHeaders.PARTITION, partition)
        .setHeader(KafkaHeaders.OFFSET, offset)
        .setHeader(KafkaHeaders.TIMESTAMP, System.currentTimeMillis())
        .build();
  }
  
}