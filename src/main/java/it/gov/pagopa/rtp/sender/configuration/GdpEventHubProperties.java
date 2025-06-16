package it.gov.pagopa.rtp.sender.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;


@ConfigurationProperties(prefix = "gdp.eventhub")
@Validated
public record GdpEventHubProperties(
    @NotBlank String name,
    @NotBlank String connectionString,
    @NotNull Consumer consumer
) {

  public record Consumer(
      @NotBlank String topic,
      @NotBlank String group
  ) {}


  @NonNull
  public String eventDispatcher() {
    return name() +
        "-" + consumer().topic()
        + "-" + consumer().group();
  }
}
