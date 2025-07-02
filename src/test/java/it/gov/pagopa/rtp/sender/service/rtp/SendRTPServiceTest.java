package it.gov.pagopa.rtp.sender.service.rtp;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtp.sender.activateClient.api.ReadApi;
import it.gov.pagopa.rtp.sender.activateClient.model.ActivationDto;
import it.gov.pagopa.rtp.sender.activateClient.model.PayerDto;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig.Activation;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig.Send;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig.Send.Retry;
import it.gov.pagopa.rtp.sender.domain.errors.MessageBadFormed;
import it.gov.pagopa.rtp.sender.domain.errors.PayerNotActivatedException;
import it.gov.pagopa.rtp.sender.domain.errors.RtpNotFoundException;
import it.gov.pagopa.rtp.sender.domain.rtp.*;
import it.gov.pagopa.rtp.sender.epcClient.model.SepaRequestToPayRequestResourceDto;
import it.gov.pagopa.rtp.sender.service.rtp.handler.SendRtpProcessor;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SendRTPServiceTest {

  @Mock
  private SepaRequestToPayMapper sepaRequestToPayMapper;
  @Mock
  private ReadApi readApi;
  private final ServiceProviderConfig serviceProviderConfig = new ServiceProviderConfig(
      "http://localhost:8080",
      new Activation("http://localhost:8080"),
      new Send("v1", new Retry(3, 100, 0.75), 10000L));
  @Mock
  private RtpRepository rtpRepository;
  @Mock
  private ObjectMapper objectMapper;
  @Mock
  private SendRtpProcessor sendRtpProcessor;

  private SendRTPServiceImpl sendRTPService;
  @Mock
  private RtpStatusUpdater rtpStatusUpdater;

  final String noticeNumber = "12345";
  final BigDecimal amount = new BigDecimal("99999999999");
  final String description = "Payment Description";
  final LocalDate expiryDate = LocalDate.now();
  final String payerId = "payerId";
  final String payeeName = "Payee Name";
  final String payerName = "Payer Name";
  final String payeeId = "payeeId";
  final String rtpSpId = "rtpSpId";
  final String iban = "IT60X0542811101000000123456";
  final String payTrxRef = "ABC/124";
  final String flgConf = "flgConf";
  final String subject = "subject";
  final String activationRtpSpId = "activationRtpSpId";

  Rtp inputRtp;

  @BeforeEach
  void setUp() {
    sendRTPService = new SendRTPServiceImpl(sepaRequestToPayMapper, readApi,
        serviceProviderConfig, rtpRepository,
        objectMapper, sendRtpProcessor, rtpStatusUpdater);
    inputRtp = Rtp.builder().noticeNumber(noticeNumber).amount(amount).description(description)
        .expiryDate(expiryDate)
        .payerId(payerId).payeeName(payeeName).payeeId(payeeId)
        .resourceID(ResourceID.createNew())
        .savingDateTime(LocalDateTime.now()).serviceProviderDebtor(rtpSpId)
        .iban(iban).payTrxRef(payTrxRef)
        .flgConf(flgConf)
        .payerName(payerName)
        .subject(subject).build();
  }

  @Test
  void testSend() {
    var fakeActivationDto = mockActivationDto();

    var expectedRtp = mockRtp();

    SepaRequestToPayRequestResourceDto mockSepaRequestToPayRequestResource = new SepaRequestToPayRequestResourceDto()
        .callbackUrl(URI.create("http://callback.url"));

    when(sepaRequestToPayMapper.toEpcRequestToPay(any()))
        .thenReturn(mockSepaRequestToPayRequestResource);
    when(readApi.findActivationByPayerId(any(), any(), any()))
        .thenReturn(Mono.just(fakeActivationDto));
    when(sendRtpProcessor.sendRtpToServiceProviderDebtor(any()))
        .thenReturn(Mono.just(expectedRtp));
    when(rtpRepository.save(any()))
        .thenReturn(Mono.just(expectedRtp));

    Mono<Rtp> result = sendRTPService.send(inputRtp);
    StepVerifier.create(result)
        .expectNextMatches(rtp -> rtp.noticeNumber().equals(expectedRtp.noticeNumber())
            && rtp.amount().equals(expectedRtp.amount())
            && rtp.description().equals(expectedRtp.description())
            && rtp.expiryDate().equals(expectedRtp.expiryDate())
            && rtp.payerId().equals(expectedRtp.payerId())
            && rtp.payerName().equals(expectedRtp.payerName())
            && rtp.payeeName().equals(expectedRtp.payeeName())
            && rtp.payeeId().equals(expectedRtp.payeeId())
            && rtp.serviceProviderDebtor().equals(expectedRtp.serviceProviderDebtor())
            && rtp.iban().equals(expectedRtp.iban())
            && rtp.payTrxRef().equals(expectedRtp.payTrxRef())
            && rtp.flgConf().equals(expectedRtp.flgConf())
            && rtp.status().equals(expectedRtp.status())
            && rtp.subject().equals(expectedRtp.subject()))
        .verifyComplete();
    verify(sepaRequestToPayMapper, times(1)).toEpcRequestToPay(any(Rtp.class));
    verify(readApi, times(1)).findActivationByPayerId(any(), any(), any());
    verify(rtpRepository, times(1)).save(any());
  }

  @Test
  void givenPayerIdNotActivatedWhenSendThenMonoError() {

    when(readApi.findActivationByPayerId(any(), any(), any()))
        .thenReturn(Mono.error(new WebClientResponseException(404, "Not Found", null, null, null)));

    Mono<Rtp> result = sendRTPService.send(inputRtp);

    StepVerifier.create(result)
        .expectError(PayerNotActivatedException.class)
        .verify();

    verify(sepaRequestToPayMapper, times(0)).toEpcRequestToPay(any(Rtp.class));
    verify(readApi, times(1)).findActivationByPayerId(any(), any(), any());
  }

  @Test
  void givenPayerIdBadFormedWhenSendThenMonoError() {
    when(readApi.findActivationByPayerId(any(), any(), any()))
        .thenReturn(Mono.error(new WebClientResponseException(400, "Bad Request", null,
            "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

    Mono<Rtp> result = sendRTPService.send(inputRtp);

    StepVerifier.create(result)
        .expectError(MessageBadFormed.class)
        .verify();

    verify(sepaRequestToPayMapper, times(0)).toEpcRequestToPay(any(Rtp.class));
    verify(readApi, times(1)).findActivationByPayerId(any(), any(), any());
  }

  @Test
  void givenInternalErrorWhenSendThenMonoError() {

    when(readApi.findActivationByPayerId(any(), any(), any()))
        .thenReturn(Mono.error(
            new WebClientResponseException(500, "Internal Server Error", null, null, null)));

    Mono<Rtp> result = sendRTPService.send(inputRtp);

    StepVerifier.create(result)
        .expectError(RuntimeException.class)
        .verify();

    verify(sepaRequestToPayMapper, times(0)).toEpcRequestToPay(any(Rtp.class));
    verify(readApi, times(1)).findActivationByPayerId(any(), any(), any());
  }

  @Test
  void givenInternalErrorOnExternalSendWhenSendThenPropagateMonoError() {
    var fakeActivationDto = mockActivationDto();
    var expectedRtp = mockRtp();

    when(readApi.findActivationByPayerId(any(), any(), any())).thenReturn(Mono.just(fakeActivationDto));

    when(sendRtpProcessor.sendRtpToServiceProviderDebtor(any()))
        .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

    when(rtpRepository.save(any()))
        .thenReturn(Mono.just(expectedRtp));

    Mono<Rtp> result = sendRTPService.send(inputRtp);

    StepVerifier.create(result)
        .expectError(UnsupportedOperationException.class)
        .verify();

    verify(rtpRepository, times(1)).save(any());
  }


  @Test
  void givenExistingCreatedRtp_whenCancelRtp_thenShouldSetCancelledStatusAndSave() {
    final var rtpId = ResourceID.createNew();
    final var createdRtp = mockRtp(RtpStatus.CREATED, rtpId, LocalDateTime.now());
    final var cancelRtp = mockRtp(RtpStatus.CANCELLED, rtpId, LocalDateTime.now());

    when(rtpRepository.findById(rtpId)).thenReturn(Mono.just(createdRtp));
    when(rtpStatusUpdater.canCancel(createdRtp)).thenReturn(Mono.just(true));
    when(sendRtpProcessor.sendRtpCancellationToServiceProviderDebtor(createdRtp))
        .thenReturn(Mono.just(cancelRtp));

    final var result = sendRTPService.cancelRtp(rtpId);

    StepVerifier.create(result)
        .assertNext(rtp -> {
          assertEquals(RtpStatus.CANCELLED, rtp.status());
          assertEquals(rtpId, rtp.resourceID());
        })
        .verifyComplete();

    verify(rtpRepository).findById(rtpId);
    verify(rtpStatusUpdater).canCancel(createdRtp);
    verify(sendRtpProcessor).sendRtpCancellationToServiceProviderDebtor(createdRtp);
  }

  @Test
  void givenNonExistingRtp_whenCancelRtp_thenShouldThrowRtpNotFoundException() {
    final var rtpId = ResourceID.createNew();

    when(rtpRepository.findById(rtpId)).thenReturn(Mono.empty());

    final var result = sendRTPService.cancelRtp(rtpId);

    StepVerifier.create(result)
        .expectError(RtpNotFoundException.class)
        .verify();

    verify(rtpRepository).findById(rtpId);
    verifyNoInteractions(sendRtpProcessor);
    verifyNoMoreInteractions(rtpRepository);
  }

  @Test
  void givenRtpInCreatedStatus_whenCancelRtp_thenShouldSucceed() {
    UUID rtpId = UUID.randomUUID();
    ResourceID resourceID = new ResourceID(rtpId);
    Rtp mockRtp = mockRtpWithStatus(RtpStatus.CREATED, rtpId);

    when(rtpRepository.findById(resourceID)).thenReturn(Mono.just(mockRtp));
    when(rtpStatusUpdater.canCancel(mockRtp)).thenReturn(Mono.just(true));
    when(sendRtpProcessor.sendRtpCancellationToServiceProviderDebtor(mockRtp)).thenReturn(Mono.just(mockRtp));

    StepVerifier.create(sendRTPService.cancelRtp(resourceID))
            .expectNext(mockRtp)
            .verifyComplete();

    verify(rtpRepository).findById(resourceID);
    verify(rtpStatusUpdater).canCancel(mockRtp);
    verify(sendRtpProcessor).sendRtpCancellationToServiceProviderDebtor(mockRtp);
  }

  @Test
  void givenRtpInNotAllowedStatus_whenCancelRtp_thenThrowsIllegalStateException() {
    UUID rtpId = UUID.randomUUID();
    ResourceID resourceID = new ResourceID(rtpId);
    Rtp mockRtp = mockRtpWithStatus(RtpStatus.PAYED, rtpId);

    when(rtpRepository.findById(resourceID)).thenReturn(Mono.just(mockRtp));
    when(rtpStatusUpdater.canCancel(mockRtp)).thenReturn(Mono.just(false));

    StepVerifier.create(sendRTPService.cancelRtp(resourceID))
            .expectErrorMatches(err ->
                    err instanceof IllegalStateException &&
                            err.getMessage().contains(rtpId.toString()))
            .verify();

    verify(rtpRepository).findById(resourceID);
    verify(rtpStatusUpdater).canCancel(mockRtp);
    verifyNoInteractions(sendRtpProcessor);
    verify(rtpRepository, never()).save(any());
  }

  @Test
  void givenNonExistentRtp_whenCancelRtp_thenThrowsRtpNotFoundException() {
    UUID rtpId = UUID.randomUUID();
    ResourceID resourceID = new ResourceID(rtpId);

    when(rtpRepository.findById(resourceID)).thenReturn(Mono.empty());

    StepVerifier.create(sendRTPService.cancelRtp(resourceID))
            .expectError(RtpNotFoundException.class)
            .verify();

    verify(rtpRepository).findById(resourceID);
    verifyNoInteractions(rtpStatusUpdater, sendRtpProcessor);
  }

  private Rtp mockRtp() {
    return mockRtp(RtpStatus.CREATED, ResourceID.createNew(), LocalDateTime.now());
  }

  private Rtp mockRtp(
      @NonNull final RtpStatus status,
      @NonNull final ResourceID resourceId,
      @NonNull final LocalDateTime savingDateTime) {
    return Rtp.builder().noticeNumber(noticeNumber).amount(amount).description(description)
        .expiryDate(expiryDate)
        .payerId(payerId).payeeName(payeeName).payeeId(payeeId)
        .payerName(payerName)
        .resourceID(resourceId)
        .savingDateTime(savingDateTime).serviceProviderDebtor(activationRtpSpId)
        .iban(iban).payTrxRef(payTrxRef)
        .status(status)
        .flgConf(flgConf)
        .subject(subject)
        .events(List.of())
        .build();
  }

  private ActivationDto mockActivationDto() {
    var spId = "activationRtpSpId";
    var fiscalCode = "activationFiscalCode";

    var payerDto = new PayerDto();
    payerDto.setRtpSpId(spId);
    payerDto.setFiscalCode(fiscalCode);

    var fakeActivationDto = new ActivationDto();
    fakeActivationDto.setId(UUID.randomUUID());
    fakeActivationDto.setEffectiveActivationDate(LocalDateTime.now());
    fakeActivationDto.setPayer(payerDto);

    return fakeActivationDto;
  }

  @Test
  void givenValidId_whenFindRtp_thenReturnRtpMono() {
    UUID rtpId = UUID.randomUUID();
    Rtp mockRtp = mock(Rtp.class);

    when(rtpRepository.findById(argThat(id -> rtpId.equals(id.getId()))))
            .thenReturn(Mono.just(mockRtp));

    StepVerifier.create(sendRTPService.findRtp(rtpId))
            .expectNext(mockRtp)
            .verifyComplete();
  }

  @Test
  void givenNonexistentId_whenFindRtp_thenThrowRtpNotFoundException() {
    UUID rtpId = UUID.randomUUID();

    when(rtpRepository.findById(argThat(id -> id.getId().equals(rtpId))))
            .thenReturn(Mono.empty());

    StepVerifier.create(sendRTPService.findRtp(rtpId))
            .expectErrorSatisfies(throwable -> assertThat(throwable)
                    .isInstanceOf(RtpNotFoundException.class)
                    .hasMessageContaining(rtpId.toString()))
            .verify();
  }

  @Test
  void givenExistingRtp_whenFindByCompositeKey_thenReturnRtp() {
    final var operationId = 100L;
    final var dispatcher = "dispatcherA";
    final var rtpId = UUID.randomUUID();
    final var rtp = Rtp.builder()
            .resourceID(new ResourceID(rtpId))
            .build();

    when(rtpRepository.findByOperationIdAndEventDispatcher(operationId, dispatcher))
            .thenReturn(Mono.just(rtp));

    StepVerifier.create(sendRTPService.findRtpByCompositeKey(operationId, dispatcher))
            .assertNext(found -> assertEquals(rtpId, found.resourceID().getId()))
            .verifyComplete();
  }

  @Test
  void givenMissingRtp_whenFindByCompositeKey_thenThrowIllegalArgumentException() {
    final var operationId = 999L;
    final var dispatcher = "nonexistent-dispatcher";

    when(rtpRepository.findByOperationIdAndEventDispatcher(operationId, dispatcher))
            .thenReturn(Mono.empty());

    StepVerifier.create(sendRTPService.findRtpByCompositeKey(operationId, dispatcher))
            .expectErrorMatches(error ->
                    error instanceof IllegalArgumentException &&
                            error.getMessage().contains("RTP not found with composite key"))
            .verify();
  }

  @Test
  void whenRepositoryFails_thenPropagateError() {
    final var operationId = 321L;
    final var dispatcher = "failing-dispatcher";

    final var exception = new RuntimeException("Database failure");

    when(rtpRepository.findByOperationIdAndEventDispatcher(operationId, dispatcher))
            .thenReturn(Mono.error(exception));

    StepVerifier.create(sendRTPService.findRtpByCompositeKey(operationId, dispatcher))
            .expectError(RuntimeException.class)
            .verify();
  }

  private Rtp mockRtpWithStatus(RtpStatus status, UUID id) {
    return Rtp.builder()
            .resourceID(new ResourceID(id))
            .status(status)
            .build();
  }


}