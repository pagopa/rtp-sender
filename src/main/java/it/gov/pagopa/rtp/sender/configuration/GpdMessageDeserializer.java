package it.gov.pagopa.rtp.sender.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class GpdMessageDeserializer implements Deserializer<GdpMessage> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public GdpMessage deserialize(String topic, byte[] data) {
    if (data == null) {
      log.warn("Received null payload from topic {}", topic);
      return null;
    }

    try {
      return objectMapper.readValue(data, GdpMessage.class);
    } catch (Exception e) {
      log.warn("Deserialization error: {}", e.getMessage());
      return null;
    }
  }
}
