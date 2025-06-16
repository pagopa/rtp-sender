package it.gov.pagopa.rtp.sender.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "gdp.eventhub")
public record GdpEventHubProperties(
    String name,
    String connectionString,
    Consumer consumer
) {

  public record Consumer(
      String topic,
      String group
  ) {}
}
