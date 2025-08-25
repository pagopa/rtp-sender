package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.registryfile.RegistryDataService;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UpdateValidOperationExceptionTest {

  @Mock
  private RegistryDataService registryDataService;

  @Mock
  private SendRTPServiceImpl sendRTPService;

  @Mock
  private GdpEventHubProperties gdpEventHubProperties;

  @InjectMocks
  private UpdateValidOperationException processor;


  @Test
  void givenValidRtpToUpdate_whenUpdateRtp_thenThrowsUnsupportedOperationException() {
    final var rtp = mock(Rtp.class);
    final var gdpMessage = mock(GdpMessage.class);

    StepVerifier.create(processor.updateRtp(rtp, gdpMessage))
        .expectErrorSatisfies(error ->
            Assertions.assertThat(error)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Update VALID existing RTP is not supported yet"))
        .verify();
  }

  @Test
  void givenRtpNotFound_whenHandleMissingRtp_thenThrowsUnsupportedOperationException() {
    final var cause = new RuntimeException("Simulated failure");
    final var gdpMessage = mock(GdpMessage.class);

    StepVerifier.create(processor.handleMissingRtp(cause, gdpMessage))
        .expectErrorSatisfies(error ->
            Assertions.assertThat(error)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Handle missing RTP for Update VALID operation is not supported yet"))
        .verify();
  }
}
