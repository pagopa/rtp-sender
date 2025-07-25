package it.gov.pagopa.rtp.sender.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.mil-auth")
@Validated
public record Oauth2ConfigProperties (
    @NotBlank String provider,
    @NotBlank String clientId,
    @NotBlank String clientSecret,
    @NotBlank String clientAuthenticationMethod,
    @NotBlank String authorizationGrantType,
    @NotBlank String clientName
) {}
