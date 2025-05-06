package it.gov.pagopa.rtp.sender.repository.rtp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("rtps")
public class RtpEntity {

  @Id
  private UUID resourceID;
  private String noticeNumber;
  private BigDecimal amount;
  private String description;
  private Instant expiryDate;
  private String payerName;
  private String payerId;
  private String payeeName;
  private String payeeId;
  private String subject;
  private Instant savingDateTime;
  private String serviceProviderDebtor;
  private String iban;
  private String payTrxRef;
  private String flgConf;
  @Field(name = "status", targetType = FieldType.STRING)
  private RtpStatus status;
  private String serviceProviderCreditor;

}
