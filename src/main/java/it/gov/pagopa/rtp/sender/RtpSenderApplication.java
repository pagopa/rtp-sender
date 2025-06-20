package it.gov.pagopa.rtp.sender;

import it.gov.pagopa.rtp.sender.configuration.*;
import it.gov.pagopa.rtp.sender.configuration.ssl.SslContextProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@EnableConfigurationProperties({
    ActivationPropertiesConfig.class,
    ServiceProviderConfig.class,
    BlobStorageConfig.class,
    CachesConfigProperties.class,
    SslContextProps.class,
    CallbackProperties.class,
    ApplicationInsightsProperties.class,
    PagoPaConfigProperties.class,
})
public class RtpSenderApplication {

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();
    SpringApplication.run(RtpSenderApplication.class, args);
  }

}