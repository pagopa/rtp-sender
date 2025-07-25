package it.gov.pagopa.rtp.sender.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;


/**
 * Configuration class for setting up OAuth2 client support using Spring Security's reactive stack.
 * <p>
 * This class registers beans necessary to enable OAuth2 client authentication using
 * the `client_credentials` or `refresh_token` grant types. It integrates with Spring WebClient
 * through a configured `ExchangeFilterFunction`.
 */
@Configuration("oauth2ClientConfig")
public class Oauth2ClientConfig {

  /**
   * Configures an {@link ExchangeFilterFunction} that automatically attaches OAuth2
   * Bearer tokens to outgoing WebClient requests, based on a given client registration ID.
   *
   * @param authorizedClientManager the manager responsible for authorizing clients and getting tokens;
   *                                must not be {@code null}
   * @param oauth2ConfigProperties  custom configuration properties that include the client registration ID;
   *                                must not be {@code null}
   * @return a filter that attaches the appropriate OAuth2 Authorization header
   */
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


  /**
   * Creates and configures a {@link ReactiveOAuth2AuthorizedClientManager} responsible
   * for managing OAuth2 client credentials and authorized client lifecycles.
   * <p>
   * It supports the {@code client_credentials} and {@code refresh_token} grant types.
   *
   * @param clientRegistrationRepository  the repository of OAuth2 client registrations; must not be {@code null}
   * @param authorizedClientRepository    the repository storing authorized client information; must not be {@code null}
   * @return the configured authorized client manager
   */
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
