package it.gov.pagopa.rtp.sender.service.oauth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import it.gov.pagopa.rtp.sender.configuration.mtlswebclient.WebClientFactory;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class Oauth2TokenServiceImpl implements Oauth2TokenService {
  private final WebClientFactory webClientFactory;

  public Oauth2TokenServiceImpl(WebClientFactory webClientFactory) {
    this.webClientFactory = webClientFactory;
  }


  @Override
  public Mono<String> getAccessToken(
      String tokenUri, String clientId, String   clientSecret, String scope, boolean isMtlsEnabled) {

    if (clientId == null || clientSecret == null || tokenUri == null) {
      return Mono.error(new IllegalStateException("OAuth2 configuration params not complete"));
    }

    log.debug("Is mTLS enabled: {}", isMtlsEnabled);
    final var webClient = isMtlsEnabled
        ? this.webClientFactory.createMtlsWebClient()
        : this.webClientFactory.createSimpleWebClient();

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "client_credentials");
    if (scope != null) {
      formData.add("scope", scope);
    }

    String credential = clientId + ":" + clientSecret;
    String encodedCredentials = Base64.getUrlEncoder().encodeToString(credential.getBytes(StandardCharsets.UTF_8));
    String authHeader = "Basic " + encodedCredentials;

    return webClient
        .post()
        .uri(tokenUri)
        .header(HttpHeaders.AUTHORIZATION, authHeader)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData(formData))
        .retrieve()
        .bodyToMono(Map.class)
        .map(response -> (String) response.get("access_token"))
        .doOnError(e -> log.error("Failed to obtain OAuth2 token {}", e.getMessage()));
  }

}
