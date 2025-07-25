package it.gov.pagopa.rtp.sender.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "client.oauth.mil-auth")
@Validated
public record Oauth2ConfigProperties (
    @NotBlank String clientId,
    @NotBlank String clientSecret,
    @NotBlank String authorizationGrantType,
    @NotBlank String tokenUri,
    @NotBlank String provider
) {}
