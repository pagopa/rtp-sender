package it.gov.pagopa.rtp.sender.domain.gdp;

import it.gov.pagopa.rtp.sender.domain.rtp.Event;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;


@Component("gdpMapper")
public class GdpMapper {

  @Nullable
  public Rtp toRtp(@Nullable final GdpMessage gdpMessage) {
    if (gdpMessage == null) {
      return null;
    }

    final var savingDateTime = Optional.of(gdpMessage)
        .map(GdpMessage::timestamp)
        .map(Instant::ofEpochMilli)
        .map(instant ->
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC))
        .orElse(null);

    return Rtp.builder()
        .resourceID(ResourceID.createNew())
        .noticeNumber(gdpMessage.iuv())
        .amount(BigDecimal.valueOf(gdpMessage.amount()))
        .description(gdpMessage.description())
        .expiryDate(gdpMessage.dueDate())
        .payerId(gdpMessage.debtorTaxCode())
        .payerName(gdpMessage.debtorTaxCode())
        .payeeId(gdpMessage.ecTaxCode())
        .payeeName(gdpMessage.ecTaxCode())
        .subject(gdpMessage.subject())
        .savingDateTime(savingDateTime)
        .status(RtpStatus.CREATED)
        .events(List.of(
            Event.builder()
                .timestamp(Instant.now())
                .triggerEvent(RtpEvent.CREATE_RTP)
                .build()
        ))
        .build();

  }


}
