package it.gov.pagopa.rtp.sender.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cloud.azure.monitor")
public record ApplicationInsightsProperties(String connectionString) {
    
}
