package it.gov.pagopa.rtp.sender.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;


@Configuration("oauth2ClientConfig")
public class Oauth2ClientConfig {

  @Bean("oauth2ClientFilter")
  public ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2ClientFilter(
      @NonNull final ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
      @NonNull final Oauth2ConfigProperties oauth2ConfigProperties) {

    final var oauth2ClientFilter =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

    oauth2ClientFilter.setDefaultClientRegistrationId(
        oauth2ConfigProperties.provider());

    return oauth2ClientFilter;
  }

  @Bean("authorizedClientManager")
  public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
      @NonNull final ReactiveClientRegistrationRepository clientRegistrationRepository,
      @NonNull final ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {

    final var authorizedClientProvider =
        ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .refreshToken()
            .build();

    final var authorizedClientManager =
        new DefaultReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientRepository);

    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

    return authorizedClientManager;
  }
}
