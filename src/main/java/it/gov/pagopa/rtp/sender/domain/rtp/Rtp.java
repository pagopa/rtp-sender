package it.gov.pagopa.rtp.sender.domain.rtp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.With;


@With
@Builder
public record Rtp(String noticeNumber, BigDecimal amount, String description, LocalDate expiryDate,
                  String payerId, String payerName, String payeeName, String payeeId,
                  ResourceID resourceID,
                  String subject, LocalDateTime savingDateTime, String serviceProviderDebtor,
                  String iban,
                  String payTrxRef, String flgConf, RtpStatus status,
                  String serviceProviderCreditor, List<Event> events,
                  Long operationId, String eventDispatcher) {}
