package it.gov.pagopa.rtp.sender.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

@Slf4j
public class GdpMessageDeserializer implements Deserializer<GdpMessage> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // No configuration needed
  }

  @Override
  public GdpMessage deserialize(String topic, byte[] data) {
    try {
      return objectMapper.readValue(data, GdpMessage.class);
    } catch (Exception e) {
      log.warn("Discarded invalid GdpMessage from topic [{}]: {}", topic, e.getMessage());
      log.debug("Invalid raw payload: {}", new String(data));
      return null; // Return null to skip the message
    }
  }

  @Override
  public void close() {
    // No cleanup necessary
  }
}

