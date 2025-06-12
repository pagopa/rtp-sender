package it.gov.pagopa.rtp.sender.controller.rtp;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import it.gov.pagopa.rtp.sender.domain.rtp.Event;
import it.gov.pagopa.rtp.sender.model.generated.send.*;
import org.springframework.stereotype.Component;

import it.gov.pagopa.rtp.sender.configuration.PagoPaConfigProperties;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;

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

  public RtpDto toRtpDto(Rtp rtp) {
    return new RtpDto().resourceID(rtp.resourceID().getId())
            .noticeNumber(rtp.noticeNumber())
            .amount(rtp.amount().doubleValue())
            .description(rtp.description())
            .expiryDate(rtp.expiryDate())
            .payerName(rtp.payerName())
            .payerId(rtp.payerId())
            .payeeName(rtp.payeeName())
            .payeeId(rtp.payeeId())
            .subject(rtp.subject())
            .savingDateTime(rtp.savingDateTime())
            .serviceProviderDebtor(rtp.serviceProviderDebtor())
            .iban(rtp.iban())
            .payTrxRef(rtp.payTrxRef())
            .flgConf(rtp.flgConf())
            .status(RtpStatusDto.valueOf(rtp.status().name()))
            .serviceProviderCreditor(rtp.serviceProviderCreditor())
            .events(toRtpEventDto(rtp.events()));
  }

  private List<EventDto> toRtpEventDto(List<Event> events) {
    return events.stream()
            .map(event -> new EventDto()
                    .timestamp(LocalDateTime.ofInstant(event.timestamp(), ZoneOffset.UTC ))
                    .precStatus( event.precStatus() != null ? RtpStatusDto.valueOf(event.precStatus().name()) : null)
                    .triggerEvent(RtpEventDto.valueOf(event.triggerEvent().name())))
            .collect(Collectors.toList());
  }

}
