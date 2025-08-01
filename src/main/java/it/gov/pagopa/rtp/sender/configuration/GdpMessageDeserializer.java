package it.gov.pagopa.rtp.sender.configuration;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;


@Component("gdpMessageDeserializer")
@Slf4j
public class GdpMessageDeserializer implements Deserializer<GdpMessage> {

  private final ObjectMapper objectMapper = new ObjectMapper();


  @Override
  public GdpMessage deserialize(String topic, byte[] data) {
    return Optional.ofNullable(data)
        .map(bytes -> {
          try {
            return objectMapper.readValue(data, GdpMessage.class);

          } catch (IOException e) {
            log.warn("Discarded invalid GdpMessage from topic [{}]: {}", topic, e.getMessage());
            log.debug("Invalid raw payload: {}", new String(data));
            return null;
          }
        })
        .orElse(null);
  }
}

