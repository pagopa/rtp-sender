package it.gov.pagopa.rtp.sender.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.exception.GdpMessageDeserializationException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class GpdMessageDeserializer implements Deserializer<GdpMessage> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public GdpMessage deserialize(String topic, byte[] data) {
    if (data == null) {
      log.warn("Received null payload on topic '{}'", topic);
      return null;
    }

    try {
      return objectMapper.readValue(data, GdpMessage.class);
    } catch (IOException e) {
      log.error("Failed to deserialize GdpMessage: Error: {}", e.getMessage());
      // Rethrow so ErrorHandlingDeserializer can handle it
      throw new GdpMessageDeserializationException("Deserialization failed", e);
    }
  }
}
