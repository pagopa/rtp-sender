package it.gov.pagopa.rtp.sender.domain.rtp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.With;


@With
@Builder
public record Rtp(String noticeNumber, BigDecimal amount, String description, LocalDate expiryDate,
    String payerId, String payerName, String payeeName, String payeeId, ResourceID resourceID,
    String subject, LocalDateTime savingDateTime, String serviceProviderDebtor, String iban,
    String payTrxRef, String flgConf, RtpStatus status, String serviceProviderCreditor) {

  public Rtp toRtpWithActivationInfo(String rtpSpId) {
    return Rtp.builder()
        .serviceProviderDebtor(rtpSpId)
        .iban(this.iban())
        .payTrxRef(this.payTrxRef())
        .flgConf(this.flgConf())
        .payerName(this.payerName())
        .payerId(this.payerId())
        .payeeName(this.payeeName())
        .payeeId(this.payeeId())
        .noticeNumber(this.noticeNumber())
        .amount(this.amount())
        .description(this.description())
        .expiryDate(this.expiryDate())
        .resourceID(this.resourceID())
        .subject(this.subject())
        .serviceProviderCreditor(this.serviceProviderCreditor())
        .savingDateTime(this.savingDateTime())
        .status(RtpStatus.CREATED)
        .build();
  }

  public Rtp toRtpSent(Rtp rtp) {
    return Rtp.builder()
        .serviceProviderDebtor(rtp.serviceProviderDebtor())
        .iban(rtp.iban())
        .payTrxRef(rtp.payTrxRef())
        .flgConf(rtp.flgConf())
        .payerName(this.payerName())
        .payerId(rtp.payerId())
        .payeeName(rtp.payeeName())
        .payeeId(rtp.payeeId())
        .noticeNumber(rtp.noticeNumber())
        .amount(rtp.amount())
        .description(rtp.description())
        .expiryDate(rtp.expiryDate())
        .resourceID(rtp.resourceID())
        .subject(this.subject())
        .serviceProviderCreditor(this.serviceProviderCreditor())
        .savingDateTime(rtp.savingDateTime())
        .status(RtpStatus.SENT)
        .build();
  }
}
