package it.gov.pagopa.rtp.sender.repository.rtp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;

@Component
public class RtpMapper {

  public Rtp toDomain(RtpEntity rtpEntity) {
    return Rtp.builder()
        .noticeNumber(rtpEntity.getNoticeNumber())
        .amount(rtpEntity.getAmount())
        .description(rtpEntity.getDescription())
        .expiryDate(LocalDate.ofInstant(rtpEntity.getExpiryDate(), ZoneOffset.UTC))
        .payerId(rtpEntity.getPayerId())
        .payerName(rtpEntity.getPayerName())
        .payeeName(rtpEntity.getPayeeName())
        .payeeId(rtpEntity.getPayeeId())
        .resourceID(new ResourceID(rtpEntity.getResourceID()))
        .savingDateTime(LocalDateTime.ofInstant(rtpEntity.getSavingDateTime(), ZoneOffset.UTC))
        .serviceProviderDebtor(rtpEntity.getServiceProviderDebtor())
        .iban(rtpEntity.getIban())
        .payTrxRef(rtpEntity.getPayTrxRef())
        .flgConf(rtpEntity.getFlgConf())
        .subject(rtpEntity.getSubject())
        .status(rtpEntity.getStatus())
        .serviceProviderCreditor(rtpEntity.getServiceProviderCreditor())
        .build();
  }

  public RtpEntity toDbEntity(Rtp rtp) {
    return RtpEntity.builder()
        .noticeNumber(rtp.noticeNumber())
        .amount(rtp.amount())
        .description(rtp.description())
        .expiryDate(rtp.expiryDate().atStartOfDay().toInstant(ZoneOffset.UTC))
        .payerId(rtp.payerId())
        .payerName(rtp.payerName())
        .payeeName(rtp.payeeName())
        .payeeId(rtp.payeeId())
        .resourceID(rtp.resourceID().getId())
        .savingDateTime(rtp.savingDateTime().toInstant(ZoneOffset.UTC))
        .serviceProviderDebtor(rtp.serviceProviderDebtor())
        .iban(rtp.iban())
        .payTrxRef(rtp.payTrxRef())
        .flgConf(rtp.flgConf())
        .subject(rtp.subject())
        .status(rtp.status())
        .serviceProviderCreditor(rtp.serviceProviderCreditor())
        .build();
  }
}
