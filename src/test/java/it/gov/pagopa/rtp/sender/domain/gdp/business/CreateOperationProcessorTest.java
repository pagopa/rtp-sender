package it.gov.pagopa.rtp.sender.domain.gdp.business;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateOperationProcessorTest {

  @Mock
  private GdpMapper gdpMapper;

  @Mock
  private SendRTPService sendRTPService;

  @InjectMocks
  private CreateOperationProcessor createOperationProcessor;


  @Test
  void givenNullMessage_whenProcessOperation_thenThrowsNullPointerException() {
    assertThatThrownBy(() -> createOperationProcessor.processOperation(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("gdpMessage must not be null");
  }

  @Test
  void givenValidMessage_whenMappingReturnsNull_thenReturnsEmptyMono() {
    final var message = GdpMessage
            .builder()
            .status(GdpMessage.Status.VALID)
            .build();
    when(gdpMapper.toRtp(message)).thenReturn(null);

    final var result = createOperationProcessor.processOperation(message);

    StepVerifier.create(result)
        .verifyComplete();

    verify(gdpMapper).toRtp(message);
    verifyNoInteractions(sendRTPService);
  }

  @Test
  void givenValidMessage_whenMappingReturnsRtp_thenSendsRtpSuccessfully() {
    final var message = GdpMessage
            .builder()
            .status(GdpMessage.Status.VALID)
            .build();
    final var rtp = mock(Rtp.class);
    final var sentRtp = mock(Rtp.class);

    when(gdpMapper.toRtp(message))
        .thenReturn(rtp);
    when(sendRTPService.send(rtp))
        .thenReturn(Mono.just(sentRtp));

    final var result = createOperationProcessor.processOperation(message);

    StepVerifier.create(result)
        .expectNext(sentRtp)
        .verifyComplete();

    verify(gdpMapper).toRtp(message);
    verify(sendRTPService).send(rtp);
  }

  @Test
  void givenValidMessage_whenSendFails_thenErrorIsPropagated() {
    final var message = GdpMessage
            .builder()
            .status(GdpMessage.Status.VALID)
            .build();
    final var rtp = mock(Rtp.class);

    when(gdpMapper.toRtp(message))
        .thenReturn(rtp);
    when(sendRTPService.send(rtp))
        .thenReturn(Mono.error(new RuntimeException("Send failed")));

    final var result = createOperationProcessor.processOperation(message);

    StepVerifier.create(result)
        .expectErrorMatches(ex -> ex instanceof RuntimeException && ex.getMessage().equals("Send failed"))
        .verify();

    verify(gdpMapper).toRtp(message);
    verify(sendRTPService).send(rtp);
  }
}
