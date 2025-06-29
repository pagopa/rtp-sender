package it.gov.pagopa.rtp.sender.controller.rtp;

import it.gov.pagopa.rtp.sender.domain.rtp.*;
import it.gov.pagopa.rtp.sender.model.generated.send.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.gov.pagopa.rtp.sender.configuration.PagoPaConfigProperties;
import it.gov.pagopa.rtp.sender.configuration.PagoPaConfigProperties.Details;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RtpDtoMapperTest {

  @Mock
  private PagoPaConfigProperties config;

  @InjectMocks
  private RtpDtoMapper rtpDtoMapper;


  @Test
  void testToRtp() {
    when(config.details()).thenReturn(new Details("iban", "fiscalCode"));
    CreateRtpDto createRtpDto = new CreateRtpDto();
    PaymentNoticeDto paymentNoticeDto = new PaymentNoticeDto();
    PayeeDto payeeDto = new PayeeDto();
    PayerDto payerDto = new PayerDto();

    paymentNoticeDto.setAmount(BigDecimal.valueOf(100));
    paymentNoticeDto.setNoticeNumber("12345");
    paymentNoticeDto.setDescription("Payment Description");
    paymentNoticeDto.setExpiryDate(LocalDate.now());
    paymentNoticeDto.setSubject("Subject");
    createRtpDto.setPaymentNotice(paymentNoticeDto);
    payerDto.setPayerId("payer123");
    payerDto.setName("John Doe");
    createRtpDto.setPayer(payerDto);
    payeeDto.setPayeeId("payee123");
    payeeDto.setName("Payee Name");
    payeeDto.setPayTrxRef("ABC/124");
    createRtpDto.setPayee(payeeDto);
    Rtp rtp = rtpDtoMapper.toRtp(createRtpDto);
    assertThat(rtp).isNotNull();
    assertThat(rtp.resourceID()).isNotNull();
    assertThat(rtp.savingDateTime()).isNotNull();
    assertThat(rtp.noticeNumber()).isEqualTo(createRtpDto.getPaymentNotice().getNoticeNumber());
    assertThat(rtp.amount()).isEqualTo(createRtpDto.getPaymentNotice().getAmount());
    assertThat(rtp.description()).isEqualTo(createRtpDto.getPaymentNotice().getDescription());
    assertThat(rtp.expiryDate()).isEqualTo(createRtpDto.getPaymentNotice().getExpiryDate());
    assertThat(rtp.payerName()).isEqualTo(createRtpDto.getPayer().getName());
    assertThat(rtp.subject()).isEqualTo(createRtpDto.getPaymentNotice().getSubject());
    assertThat(rtp.payerId()).isEqualTo(createRtpDto.getPayer().getPayerId());
    assertThat(rtp.payeeName()).isEqualTo(createRtpDto.getPayee().getName());
    assertThat(rtp.payeeId()).isEqualTo(createRtpDto.getPayee().getPayeeId());
    assertThat(rtp.serviceProviderDebtor()).isEqualTo("serviceProviderDebtor");
    assertThat(rtp.iban()).isEqualTo(config.details().iban());
    assertThat(rtp.payTrxRef()).isEqualTo(createRtpDto.getPayee().getPayTrxRef());
    assertThat(rtp.flgConf()).isEqualTo("flgConf");
  }


  @Test
  void testTtoRtpWithSpCroRtp() {
    when(config.details()).thenReturn(new Details("iban", "fiscalCode"));
    CreateRtpDto createRtpDto = new CreateRtpDto();
    PaymentNoticeDto paymentNoticeDto = new PaymentNoticeDto();
    PayeeDto payeeDto = new PayeeDto();
    PayerDto payerDto = new PayerDto();
    String subject = "PagoPA";

    paymentNoticeDto.setAmount(BigDecimal.valueOf(100));
    paymentNoticeDto.setNoticeNumber("12345");
    paymentNoticeDto.setDescription("Payment Description");
    paymentNoticeDto.setExpiryDate(LocalDate.now());
    paymentNoticeDto.setSubject("Subject");
    createRtpDto.setPaymentNotice(paymentNoticeDto);
    payerDto.setPayerId("payer123");
    payerDto.setName("John Doe");
    createRtpDto.setPayer(payerDto);
    payeeDto.setPayeeId("payee123");
    payeeDto.setName("Payee Name");
    payeeDto.setPayTrxRef("ABC/124");
    createRtpDto.setPayee(payeeDto);

    Rtp rtp = rtpDtoMapper.toRtpWithServiceProviderCreditor(createRtpDto,subject);
    assertThat(rtp).isNotNull();
    assertThat(rtp.resourceID()).isNotNull();
    assertThat(rtp.savingDateTime()).isNotNull();
    assertThat(rtp.noticeNumber()).isEqualTo(createRtpDto.getPaymentNotice().getNoticeNumber());
    assertThat(rtp.amount()).isEqualTo(createRtpDto.getPaymentNotice().getAmount());
    assertThat(rtp.description()).isEqualTo(createRtpDto.getPaymentNotice().getDescription());
    assertThat(rtp.expiryDate()).isEqualTo(createRtpDto.getPaymentNotice().getExpiryDate());
    assertThat(rtp.payerName()).isEqualTo(createRtpDto.getPayer().getName());
    assertThat(rtp.subject()).isEqualTo(createRtpDto.getPaymentNotice().getSubject());
    assertThat(rtp.payerId()).isEqualTo(createRtpDto.getPayer().getPayerId());
    assertThat(rtp.payeeName()).isEqualTo(createRtpDto.getPayee().getName());
    assertThat(rtp.payeeId()).isEqualTo(createRtpDto.getPayee().getPayeeId());
    assertThat(rtp.serviceProviderDebtor()).isEqualTo("serviceProviderDebtor");
    assertThat(rtp.iban()).isEqualTo(config.details().iban());
    assertThat(rtp.payTrxRef()).isEqualTo(createRtpDto.getPayee().getPayTrxRef());
    assertThat(rtp.flgConf()).isEqualTo("flgConf");
    assertThat(rtp.serviceProviderCreditor()).isEqualTo(subject);
  }
  
  @Test
  void givenValidRtp_whenToRtpDto_thenCorrectlyMapped() {
    UUID uuid = UUID.randomUUID();
    Instant nowInstant = Instant.now();
    LocalDateTime nowLdt = LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC);
    LocalDate expiry = LocalDate.now().plusDays(10);

    Event event = new Event(nowInstant, RtpStatus.CREATED, RtpEvent.SEND_RTP);
    Rtp rtp = new Rtp(
            "123456789",
            BigDecimal.valueOf(150.75),
            "Pagamento TARI",
            expiry,
            "payer-001",
            "Mario Rossi",
            "Comune di Roma",
            "payee-002",
            new ResourceID(uuid),
            "TARI 2025",
            nowLdt,
            "DEBTOR-001",
            "IT60X0542811101000000123456",
            "TX123456",
            "Y",
            RtpStatus.SENT,
            "CREDITOR-001",
            List.of(event),
            1L,
            "eventDispatcher"
    );

    RtpDto dto = rtpDtoMapper.toRtpDto(rtp);

    assertNotNull(dto);
    assertEquals(uuid, dto.getResourceID());
    assertEquals("123456789", dto.getNoticeNumber());
    assertEquals(150.75, dto.getAmount());
    assertEquals("Pagamento TARI", dto.getDescription());
    assertEquals(expiry, dto.getExpiryDate());
    assertEquals("Mario Rossi", dto.getPayerName());
    assertEquals("payer-001", dto.getPayerId());
    assertEquals("Comune di Roma", dto.getPayeeName());
    assertEquals("payee-002", dto.getPayeeId());
    assertEquals("TARI 2025", dto.getSubject());
    assertEquals(nowLdt, dto.getSavingDateTime());
    assertEquals("DEBTOR-001", dto.getServiceProviderDebtor());
    assertEquals("IT60X0542811101000000123456", dto.getIban());
    assertEquals("TX123456", dto.getPayTrxRef());
    assertEquals("Y", dto.getFlgConf());
    assertEquals(RtpStatusDto.SENT, dto.getStatus());
    assertEquals("CREDITOR-001", dto.getServiceProviderCreditor());

    assertNotNull(dto.getEvents());
    assertEquals(1, dto.getEvents().size());
    EventDto eventDto = dto.getEvents().getFirst();
    assertEquals(LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC), eventDto.getTimestamp());
    assertEquals(RtpStatusDto.CREATED, eventDto.getPrecStatus());
    assertEquals(RtpEventDto.SEND_RTP, eventDto.getTriggerEvent());
  }
}