package it.gov.pagopa.rtp.sender.controller.rtp;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import it.gov.pagopa.rtp.sender.configuration.PagoPaConfigProperties;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.model.generated.send.CreateRtpDto;

@Component
public class RtpDtoMapper {

  private final PagoPaConfigProperties config;

  public RtpDtoMapper(PagoPaConfigProperties config) {
    this.config = config;
  }

    public Rtp toRtp(CreateRtpDto createRtpDto) {

    return Rtp.builder().noticeNumber(createRtpDto.getPaymentNotice().getNoticeNumber())
        .amount(createRtpDto.getPaymentNotice().getAmount()).resourceID(ResourceID.createNew())
        .description(createRtpDto.getPaymentNotice().getDescription())
        .expiryDate(createRtpDto.getPaymentNotice().getExpiryDate())
        .savingDateTime(LocalDateTime.now())
        .payerName(createRtpDto.getPayer().getName())
        .payerId(createRtpDto.getPayer().getPayerId()).payeeName(createRtpDto.getPayee().getName())
        .payeeId(createRtpDto.getPayee().getPayeeId()).serviceProviderDebtor("serviceProviderDebtor").iban(config.details().iban())
        .subject(createRtpDto.getPaymentNotice().getSubject())
        .payTrxRef(createRtpDto.getPayee().getPayTrxRef()).flgConf("flgConf").build();
  }

  public Rtp toRtpWithServiceProviderCreditor(CreateRtpDto createRtpDto, String tokenSub) {
    return Rtp.builder().noticeNumber(createRtpDto.getPaymentNotice().getNoticeNumber())
        .amount(createRtpDto.getPaymentNotice().getAmount()).resourceID(ResourceID.createNew())
        .description(createRtpDto.getPaymentNotice().getDescription())
        .expiryDate(createRtpDto.getPaymentNotice().getExpiryDate())
        .savingDateTime(LocalDateTime.now())
        .payerName(createRtpDto.getPayer().getName())
        .payerId(createRtpDto.getPayer().getPayerId()).payeeName(createRtpDto.getPayee().getName())
        .payeeId(createRtpDto.getPayee().getPayeeId()).serviceProviderDebtor("serviceProviderDebtor").iban(config.details().iban())
        .subject(createRtpDto.getPaymentNotice().getSubject())
        .serviceProviderCreditor(tokenSub)
        .payTrxRef(createRtpDto.getPayee().getPayTrxRef()).flgConf("flgConf").build();
  }

}
