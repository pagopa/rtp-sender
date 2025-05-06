package it.gov.pagopa.rtp.sender.statemachine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;


@ExtendWith(MockitoExtension.class)
class RtpTransitionConfigurationTest {

  @Mock
  private RtpTransition transition;

  @Test
  void givenValidKey_whenGetTransition_thenReturnTransition() {
    final var key = new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.SEND_RTP);
    final var transitions = Map.of(key, transition);
    final var configuration = new RtpTransitionConfiguration(transitions);

    final var result = configuration.getTransition(key);

    assertTrue(result.isPresent());
    assertEquals(transition, result.get());
  }

  @Test
  void givenInvalidKey_whenGetTransition_thenReturnEmpty() {
    final var validKey = new RtpTransitionKey(RtpStatus.CREATED, RtpEvent.SEND_RTP);
    final var invalidKey = new RtpTransitionKey(RtpStatus.SENT, RtpEvent.ERROR_SEND_RTP);
    final var transitions = Map.of(validKey, transition);
    final var configuration = new RtpTransitionConfiguration(transitions);

    final var result = configuration.getTransition(invalidKey);

    assertTrue(result.isEmpty());
  }

  @Test
  void givenNullKey_whenGetTransition_thenThrowNullPointerException() {
    final var configuration = new RtpTransitionConfiguration(Map.of());

    assertThrows(NullPointerException.class, () -> configuration.getTransition(null));
  }
}
