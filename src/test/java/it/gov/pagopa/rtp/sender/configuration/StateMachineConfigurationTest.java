package it.gov.pagopa.rtp.sender.configuration;

import static org.junit.jupiter.api.Assertions.*;

import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig.Send;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig.Send.Retry;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpDB;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpEntity;
import it.gov.pagopa.rtp.sender.statemachine.RtpTransitionKey;
import it.gov.pagopa.rtp.sender.statemachine.TransitionConfiguration;

@ExtendWith(MockitoExtension.class)
class StateMachineConfigurationTest {

  @Mock
  private RtpDB rtpDB;

  private StateMachineConfiguration configuration;

  @BeforeEach
  void setUp() {
    final var retryConfig = new Retry(3, 1L, 0.1D);

    final var serviceProviderConfig = new ServiceProviderConfig(
        null, null, new Send(null, retryConfig, null));


    configuration = new StateMachineConfiguration(rtpDB, serviceProviderConfig);
  }


  @ParameterizedTest
  @MethodSource("transitionArguments")
  void givenStateMachineConfig_whenCreatingConfigurer_thenTransitionsAreRegistered(
      RtpStatus from, RtpEvent event, RtpStatus to
  ) {
    final var configurer = configuration.transitionConfigurer();
    final var config = configurer.build();

    assertTransitionExists(config, from, event, to);
  }


  private static Stream<Arguments> transitionArguments() {
    return Stream.of(
        Arguments.of(RtpStatus.CREATED, RtpEvent.SEND_RTP, RtpStatus.SENT),
        Arguments.of(RtpStatus.CREATED, RtpEvent.ERROR_SEND_RTP, RtpStatus.ERROR_SEND),
        Arguments.of(RtpStatus.CREATED, RtpEvent.ACCEPT_RTP, RtpStatus.ACCEPTED),
        Arguments.of(RtpStatus.CREATED, RtpEvent.REJECT_RTP, RtpStatus.REJECTED),
        Arguments.of(RtpStatus.CREATED, RtpEvent.USER_ACCEPT_RTP, RtpStatus.USER_ACCEPTED),
        Arguments.of(RtpStatus.CREATED, RtpEvent.USER_REJECT_RTP, RtpStatus.USER_REJECTED),
        Arguments.of(RtpStatus.CREATED, RtpEvent.PAY_RTP, RtpStatus.PAID),
        Arguments.of(RtpStatus.CREATED, RtpEvent.CANCEL_RTP, RtpStatus.CANCELLED),
        Arguments.of(RtpStatus.CREATED, RtpEvent.CANCEL_RTP_PAID, RtpStatus.CANCELLED_PAID),
        Arguments.of(RtpStatus.SENT, RtpEvent.ACCEPT_RTP, RtpStatus.ACCEPTED),
        Arguments.of(RtpStatus.SENT, RtpEvent.REJECT_RTP, RtpStatus.REJECTED),
        Arguments.of(RtpStatus.SENT, RtpEvent.USER_ACCEPT_RTP, RtpStatus.USER_ACCEPTED),
        Arguments.of(RtpStatus.SENT, RtpEvent.USER_REJECT_RTP, RtpStatus.USER_REJECTED),
        Arguments.of(RtpStatus.SENT, RtpEvent.PAY_RTP, RtpStatus.PAID),
        Arguments.of(RtpStatus.SENT, RtpEvent.CANCEL_RTP, RtpStatus.CANCELLED),
        Arguments.of(RtpStatus.SENT, RtpEvent.CANCEL_RTP_PAID, RtpStatus.CANCELLED_PAID),
        Arguments.of(RtpStatus.ACCEPTED, RtpEvent.USER_ACCEPT_RTP, RtpStatus.USER_ACCEPTED),
        Arguments.of(RtpStatus.ACCEPTED, RtpEvent.USER_REJECT_RTP, RtpStatus.USER_REJECTED),
        Arguments.of(RtpStatus.ACCEPTED, RtpEvent.CANCEL_RTP, RtpStatus.CANCELLED),
        Arguments.of(RtpStatus.ACCEPTED, RtpEvent.CANCEL_RTP_PAID, RtpStatus.CANCELLED_PAID),
        Arguments.of(RtpStatus.USER_ACCEPTED, RtpEvent.PAY_RTP, RtpStatus.PAID),
        Arguments.of(RtpStatus.USER_ACCEPTED, RtpEvent.CANCEL_RTP, RtpStatus.CANCELLED),
        Arguments.of(RtpStatus.USER_ACCEPTED, RtpEvent.CANCEL_RTP_PAID, RtpStatus.CANCELLED_PAID),
        Arguments.of(RtpStatus.CANCELLED, RtpEvent.CANCEL_RTP_ACCR, RtpStatus.CANCELLED_ACCR),
        Arguments.of(RtpStatus.CANCELLED, RtpEvent.CANCEL_RTP_REJECTED, RtpStatus.CANCELLED_REJECTED),
        Arguments.of(RtpStatus.CANCELLED, RtpEvent.ERROR_CANCEL_RTP, RtpStatus.ERROR_CANCEL)
    );
  }


  private void assertTransitionExists(
      TransitionConfiguration<RtpEntity, RtpStatus, RtpEvent> config,
      RtpStatus source,
      RtpEvent event,
      RtpStatus expectedTarget
  ) {
    final var key = new RtpTransitionKey(source, event);
    final var transition = config.getTransition(key);

    assertTrue(transition.isPresent(), "Expected transition not found for: " + key);
    assertEquals(expectedTarget, transition.get().getDestination(), "Unexpected destination for: " + key);

    final var postActions = transition.get()
        .getPostTransactionActions();

    assertFalse(postActions.isEmpty(), "No post-actions found for: " + key);
  }
}
