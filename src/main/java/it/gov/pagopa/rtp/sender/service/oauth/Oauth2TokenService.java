package it.gov.pagopa.rtp.sender.service.oauth;

import reactor.core.publisher.Mono;

public interface Oauth2TokenService {
  Mono<String> getAccessToken(String tokenUri, String clientId, String clientSecret, String scope,
      boolean isMtlsEnabled);
}
