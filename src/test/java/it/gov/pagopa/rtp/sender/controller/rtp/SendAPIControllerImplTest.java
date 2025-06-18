package it.gov.pagopa.rtp.sender.controller.rtp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import it.gov.pagopa.rtp.sender.activateClient.model.ErrorDto;
import it.gov.pagopa.rtp.sender.activateClient.model.ErrorsDto;
import it.gov.pagopa.rtp.sender.configuration.SecurityConfig;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig;
import it.gov.pagopa.rtp.sender.domain.errors.*;
import it.gov.pagopa.rtp.sender.domain.rtp.*;
import it.gov.pagopa.rtp.sender.model.generated.send.*;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import it.gov.pagopa.rtp.sender.utils.Users.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {SendAPIControllerImpl.class})
@Import({SecurityConfig.class})
@DisabledInAotMode
class SendAPIControllerImplTest {

  @MockitoBean
  private SendRTPService sendRTPService;

  @MockitoBean
  private RtpDtoMapper rtpDtoMapper;

  @Autowired
  private ServiceProviderConfig serviceProviderConfig;

  private WebTestClient webTestClient;

  @Autowired
  private ApplicationContext context;

  private Rtp expectedRtp;

  @BeforeEach
  void setup() {
    String noticeNumber = "12345";
    BigDecimal amount = new BigDecimal("99999999999");
    String description = "Payment Description";
    LocalDate expiryDate = LocalDate.now();
    String payerId = "payerId";
    String payeeName = "Payee Name";
    String payeeId = "payeeId";
    String rtpSpId = "serviceProviderDebtor";
    String iban = "IT60X0542811101000000123456";
    String flgConf = "flgConf";
    String payerName = "John Doe";
    String payTrxRef = "ABC/124";
    String subject = "subject";
    String serviceProviderCreditor = "Pagopa";

    expectedRtp = Rtp.builder().noticeNumber(noticeNumber).amount(amount).description(description)
        .expiryDate(expiryDate)
        .payerId(payerId).payeeName(payeeName).payeeId(payeeId)
        .resourceID(ResourceID.createNew())
        .savingDateTime(LocalDateTime.now()).serviceProviderDebtor(rtpSpId)
        .payerName(payerName)
        .subject(subject)
        .serviceProviderCreditor(serviceProviderCreditor)
        .iban(iban).payTrxRef(payTrxRef)
        .flgConf(flgConf).build();

    webTestClient = WebTestClient
        .bindToApplicationContext(context)
        .apply(springSecurity())
        .configureClient()
        .build();
  }

  @Test
  @RtpSenderWriter()
  void testSendRtpSuccessful() {

    when(rtpDtoMapper.toRtpWithServiceProviderCreditor(any(CreateRtpDto.class),
        eq("PagoPA"))).thenReturn(expectedRtp);
    when(sendRTPService.send(expectedRtp)).thenReturn(Mono.just(expectedRtp));

    webTestClient
        .post()
        .uri("/rtps")
        .bodyValue(generateSendRequest())
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .location("http://localhost:8080/rtp/rtps/" + expectedRtp.resourceID().getId())
        .expectBody()
        .isEmpty();

    verify(sendRTPService, times(1)).send(expectedRtp);
  }

  @Test
  @RtpSenderWriter
  void testSendRtpWithWrongBody() {

    when(rtpDtoMapper.toRtpWithServiceProviderCreditor(any(CreateRtpDto.class),
        anyString())).thenReturn(expectedRtp);
    when(sendRTPService.send(any()))
        .thenReturn(Mono.empty());

    webTestClient.post()
        .uri("/rtps")
        .bodyValue(generateWrongSendRequest())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @RtpSenderWriter
  void testSendRtpWithWrongAmount() {

    when(rtpDtoMapper.toRtpWithServiceProviderCreditor(any(CreateRtpDto.class),
        anyString())).thenReturn(expectedRtp);
    when(sendRTPService.send(any()))
        .thenReturn(Mono.empty());

    webTestClient.post()
        .uri("/rtps")
        .bodyValue(generateWrongAmountSendRequest())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.BAD_REQUEST);

    verify(sendRTPService, times(0)).send(any());
    verify(rtpDtoMapper, times(0)).toRtp(any());
  }

  @Test
  @WithMockUser
  void userWithoutEnoughPermissionShouldNotSendRtp() {
    webTestClient.post()
        .uri("/rtps")
        .bodyValue(generateSendRequest())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @RtpSenderWriter
  void givenUserNotActivatedWhenSendRTPThenReturnUnprocessableEntity() {

    when(rtpDtoMapper.toRtpWithServiceProviderCreditor(any(CreateRtpDto.class),
        anyString())).thenReturn(expectedRtp);
    when(sendRTPService.send(any()))
        .thenReturn(Mono.error(new PayerNotActivatedException()));

    webTestClient.post()
        .uri("/rtps")
        .bodyValue(generateSendRequest())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    verify(sendRTPService, times(1)).send(any());
    verify(rtpDtoMapper, times(1)).toRtpWithServiceProviderCreditor(any(), (any()));
  }

  @Test
  @RtpSenderWriter
  void givenMessageBadFormedWhenSendRTPThenReturnBadRequest() {

    when(rtpDtoMapper.toRtpWithServiceProviderCreditor(any(CreateRtpDto.class),
        anyString())).thenReturn(expectedRtp);
    when(sendRTPService.send(any()))
        .thenReturn(Mono.error(generateMessageBadFormed()));

    webTestClient.post()
        .uri("/rtps")
        .bodyValue(generateSendRequest())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.BAD_REQUEST);

    verify(sendRTPService, times(1)).send(any());
    verify(rtpDtoMapper, times(1)).toRtpWithServiceProviderCreditor(any(), (any()));
  }

  @Test
  @RtpSenderWriter
  void givenBadFiscalCodeWhenSendRTPThenReturnBadRequest() {

    webTestClient.post()
        .uri("/rtps")
        .bodyValue(generateWrongSendRequest())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.BAD_REQUEST);

    verify(sendRTPService, times(0)).send(any());
    verify(rtpDtoMapper, times(0)).toRtpWithServiceProviderCreditor(any(), (any()));
  }

  @Test
  @RtpSenderWriter
  void givenBadExpiryDate_whenSendRTP_thenReturnBadRequest() {

    String invalidJson = """
        {
            "payee": {
                "name": "Comune di Smartino",
                "payeeId": "77777777777",
                "payTrxRef": "ABC/124"
            },
            "payer": {
                "name": "Pigrolo",
                "payerId": "NNAPRL01D01H501T"
            },
            "paymentNotice": {
                "noticeNumber": "311111111112222222",
                "description": "Paga questo avviso",
                "subject": "TARI 2025",
                "amount": 40000,
                "expiryDate": "UNPARSABLE"
            }
        }
        """;

    // When: Sending a POST request with invalid type
    webTestClient.post()
        .uri("/rtps")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidJson)
        .exchange()
        // Then: Verify the response
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.error").isEqualTo("Malformed request")
        .jsonPath("$.details").exists();

    verify(sendRTPService, times(0)).send(any());
    verify(rtpDtoMapper, times(0)).toRtpWithServiceProviderCreditor(any(), (any()));

  }


  @Test
  @RtpSenderWriter()
  void givenRejectedRtp_whenSendRtp_thenReturnUnprocessableEntity() {

    when(rtpDtoMapper.toRtpWithServiceProviderCreditor(any(CreateRtpDto.class),
        eq("PagoPA"))).thenReturn(expectedRtp);
    when(sendRTPService.send(expectedRtp))
        .thenReturn(Mono.error(new SepaRequestException("Rejected")));

    webTestClient.post()
        .uri("/rtps")
        .bodyValue(generateSendRequest())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    verify(sendRTPService, times(1)).send(expectedRtp);
  }


  @Test
  @RtpSenderWriter
  void givenValidRtpId_whenCancelRtp_thenReturnNoContent() {
    final var rtpId = UUID.randomUUID();

    final var cancelledRtp = Rtp.builder()
        .resourceID(new ResourceID(rtpId))
        .status(RtpStatus.CANCELLED)
        .build();

    when(sendRTPService.cancelRtp(any(ResourceID.class)))
        .thenReturn(Mono.just(cancelledRtp));

    webTestClient.post()
        .uri("/rtps//{rtpId}/cancel", rtpId)
        .header("RequestId", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isNoContent();
  }


  @Test
  @RtpSenderWriter
  void givenNonExistingRtpId_whenCancelRtp_thenReturnNotFound() {
    final var rtpId = UUID.randomUUID();

    when(sendRTPService.cancelRtp(any(ResourceID.class)))
        .thenReturn(Mono.error(new RtpNotFoundException(rtpId)));

    webTestClient.post()
        .uri("/rtps/{rtpId}/cancel", rtpId)
        .header("RequestId", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isNotFound();
  }


  @Test
  @RtpSenderWriter
  void givenUnexpectedError_whenCancelRtp_thenReturnInternalServerError() {
    final var rtpId = UUID.randomUUID();

    when(sendRTPService.cancelRtp(any(ResourceID.class)))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

    webTestClient.post()
        .uri("/rtps/{rtpId}/cancel", rtpId)
        .header("RequestId", UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().is5xxServerError();
  }

  @Test
  @RtpSenderWriter
  void givenInvalidRtpStatus_whenCancelRtp_thenReturnUnprocessableEntity() {
    final var rtpId = UUID.randomUUID();

    when(sendRTPService.cancelRtp(any(ResourceID.class)))
            .thenReturn(Mono.error(new InvalidRtpStatusException(rtpId, RtpStatus.SENT)));

    webTestClient.post()
            .uri("/rtps/{rtpId}/cancel", rtpId)
            .header("RequestId", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @Test
  @RtpSenderReader
  void givenValidId_whenFindRtpById_thenReturnDtoInResponseEntityOk() {
    UUID requestId = UUID.randomUUID();
    Rtp rtp = generateRtp();
    RtpDto dto = generateRtpDto();

    when(sendRTPService.findRtp(rtp.resourceID().getId())).thenReturn(Mono.just(rtp));
    when(rtpDtoMapper.toRtpDto(rtp)).thenReturn(dto);

    webTestClient.get()
            .uri("/rtps/{id}", rtp.resourceID().getId())
            .header("requestId", requestId.toString())
            .header("api-version", "v1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(RtpDto.class)
            .isEqualTo(dto);
  }

  @Test
  @RtpSenderReader
  void givenNonexistentId_whenFindRtpById_thenReturnNotFound() {
    UUID requestId = UUID.randomUUID();
    UUID rtpId = UUID.randomUUID();

    when(sendRTPService.findRtp(rtpId)).thenReturn(Mono.error(new RtpNotFoundException(rtpId)));

    webTestClient.get()
            .uri("/rtps/{id}", rtpId)
            .header("requestId", requestId.toString())
            .header("api-version", "v1")
            .exchange()
            .expectStatus().isNotFound();
  }

  @Test
  @RtpSenderReader
  void givenUnexpectedError_whenFindRtpById_thenReturnServerError() {
    UUID requestId = UUID.randomUUID();
    UUID rtpId = UUID.randomUUID();

    when(sendRTPService.findRtp(rtpId)).thenReturn(Mono.error(new RuntimeException("Something went wrong")));

    webTestClient.get()
            .uri("/rtps/{id}", rtpId)
            .header("requestId", requestId.toString())
            .header("api-version", "v1")
            .exchange()
            .expectStatus().is5xxServerError();
  }

  @Test
  @WithMockUser
  void whenUserWithoutRoleAccessesFindRtpById_thenAccessDenied() {
    UUID requestId = UUID.randomUUID();
    UUID rtpId = UUID.randomUUID();

    webTestClient.get()
            .uri("/rtps/{id}", rtpId)
            .header("requestId", requestId.toString())
            .header("api-version", "v1")
            .exchange()
            .expectStatus().isForbidden();
  }

  private CreateRtpDto generateSendRequest() {

    PayeeDto payeeDto = new PayeeDto();

    PayerDto payerDto = new PayerDto();

    PaymentNoticeDto paymentNoticeDto = new PaymentNoticeDto();

    payeeDto.setName("payeeName");
    payeeDto.setPayeeId("77777777777");
    payeeDto.setPayTrxRef("ABC/124");

    payerDto.setName("payerName");
    payerDto.setPayerId("12345678911");

    paymentNoticeDto.setAmount(BigDecimal.valueOf(1));
    paymentNoticeDto.setSubject("subject");
    paymentNoticeDto.setDescription("description");
    paymentNoticeDto.setNoticeNumber("311111111112222222");
    paymentNoticeDto.setExpiryDate(LocalDate.now());

    return new CreateRtpDto(payeeDto, payerDto, paymentNoticeDto);
  }

  private CreateRtpDto generateWrongSendRequest() {
    PayeeDto payeeDto = new PayeeDto();

    PayerDto payerDto = new PayerDto();

    PaymentNoticeDto paymentNoticeDto = new PaymentNoticeDto();

    payeeDto.setName("payeeName");
    payeeDto.setPayeeId("77777777777");
    payeeDto.setPayTrxRef("ABC/124");

    payerDto.setName("payername");
    payerDto.setPayerId("badfiscalcode");

    paymentNoticeDto.setAmount(BigDecimal.valueOf(1));
    paymentNoticeDto.setSubject("subject");
    paymentNoticeDto.setDescription("description");
    paymentNoticeDto.setNoticeNumber("noticenumber");
    paymentNoticeDto.setExpiryDate(LocalDate.now());

    return new CreateRtpDto(payeeDto, payerDto, paymentNoticeDto);
  }

  private CreateRtpDto generateWrongAmountSendRequest() {
    PayeeDto payeeDto = new PayeeDto();

    PayerDto payerDto = new PayerDto();

    PaymentNoticeDto paymentNoticeDto = new PaymentNoticeDto();

    payeeDto.setName("payeeName");
    payeeDto.setPayeeId("77777777777");
    payeeDto.setPayTrxRef("ABC/124");

    payerDto.setName("payername");
    payerDto.setPayerId("payerId");

    paymentNoticeDto.setAmount(new BigDecimal("999999999999"));
    paymentNoticeDto.setSubject("subject");
    paymentNoticeDto.setDescription("description");
    paymentNoticeDto.setNoticeNumber("311111111112222222");
    paymentNoticeDto.setExpiryDate(LocalDate.now());

    return new CreateRtpDto(payeeDto, payerDto, paymentNoticeDto);

  }

  private MessageBadFormed generateMessageBadFormed() {
    var errors = new ErrorsDto();
    var newError = new ErrorDto();
    newError.setDescription("description");
    newError.setCode("code");
    errors.addErrorsItem(newError);
    return new MessageBadFormed(errors);
  }

  private Rtp generateRtp() {

    UUID uuid = UUID.fromString("76a185a7-4f8f-44ad-b08e-2da722e25ff8");
    LocalDateTime localDateTime = LocalDateTime.of(2025, 1, 1, 1, 0, 0);
    Instant dateInstant = localDateTime.toInstant(ZoneOffset.UTC);
    LocalDate expiry = LocalDate.of(2025, 1, 1);
    Event event = new Event(dateInstant, RtpStatus.CREATED, RtpEvent.SEND_RTP);

    return Rtp.builder()
            .noticeNumber("123456789")
            .amount(BigDecimal.valueOf(150.75))
            .description("Pagamento TARI")
            .expiryDate(expiry)
            .payerId("payer-001")
            .payerName("Mario Rossi")
            .payeeName("Comune di Roma")
            .payeeId("payee-002")
            .resourceID(new ResourceID(uuid))
            .subject("TARI 2025")
            .savingDateTime(localDateTime)
            .serviceProviderDebtor("DEBTOR-001")
            .iban("IT60X0542811101000000123456")
            .payTrxRef("TX123456")
            .flgConf("Y")
            .status(RtpStatus.SENT)
            .serviceProviderCreditor("CREDITOR-001")
            .events(List.of(event))
            .build();
  }

  private RtpDto generateRtpDto() {

    UUID uuid = UUID.fromString("76a185a7-4f8f-44ad-b08e-2da722e25ff8");
    LocalDateTime date = LocalDateTime.of(2025,1,1,1,0,0);
    LocalDate expiry = LocalDate.of(2025,1,1);
    EventDto event = new EventDto()
            .timestamp(date)
            .precStatus(RtpStatusDto.CREATED)
            .triggerEvent(RtpEventDto.SEND_RTP);

    return new RtpDto()
            .noticeNumber("123456789")
            .amount(150.75)
            .expiryDate(expiry)
            .payerId("payer-001")
            .payerName("Mario Rossi")
            .payeeName("Comune di Roma")
            .payeeId("payee-002")
            .resourceID(uuid)
            .subject("TARI 2025")
            .savingDateTime(date)
            .serviceProviderDebtor("DEBTOR-001")
            .iban("IT60X0542811101000000123456")
            .payTrxRef("TX123456")
            .flgConf("Y")
            .status(RtpStatusDto.SENT)
            .serviceProviderCreditor("CREDITOR-001")
            .events(List.of(event));
  }
}
