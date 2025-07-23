package it.gov.pagopa.rtp.sender.domain.gdp;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.configuration.PagoPaConfigProperties;
import it.gov.pagopa.rtp.sender.domain.rtp.Event;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpEvent;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;


/**
 * Component responsible for mapping {@link GdpMessage} instances into {@link Rtp} domain objects.
 * <p>
 * This mapper is to be used in message processing flows to convert incoming GDP messages into
 * an internal RTP representation used throughout the application.
 * </p>
 */
@Component("gdpMapper")
@Slf4j
public class GdpMapper {

  private final GdpEventHubProperties gdpEventHubProperties;
  private final PagoPaConfigProperties pagoPaConfigProperties;


  /**
   * Constructs a new {@link GdpMapper} with the given {@link GdpEventHubProperties}.
   *
   * @param gdpEventHubProperties configuration properties related to the Event Hub; must not be null
   * @throws NullPointerException if {@code gdpEventHubProperties} is null
   */
  public GdpMapper(
          @NonNull final GdpEventHubProperties gdpEventHubProperties,
          @NonNull final PagoPaConfigProperties pagoPaConfigProperties) {

    this.gdpEventHubProperties = Objects.requireNonNull(gdpEventHubProperties);
    this.pagoPaConfigProperties = Objects.requireNonNull(pagoPaConfigProperties);
  }


  /**
   * Maps a {@link GdpMessage} to a new {@link Rtp} instance.
   *
   * @param gdpMessage the source GDP message to convert; may be {@code null}
   * @return a new {@link Rtp} instance mapped from the given {@link GdpMessage}, or {@code null} if input is {@code null}
   */
  @Nullable
  public Rtp toRtp(
      @Nullable final GdpMessage gdpMessage) {

    if (gdpMessage == null) {
      return null;
    }

    final var savingDateTime = Optional.of(gdpMessage)
        .map(GdpMessage::timestamp)
        .map(Instant::ofEpochMilli)
        .map(instant ->
            LocalDateTime.ofInstant(instant, ZoneOffset.UTC))
        .orElse(null);

    final var expiryDate = Optional.ofNullable(gdpMessage.due_date())
        .map(this::convertLongToLocalDate)
        .orElse(null);

    return Rtp.builder()
        .resourceID(ResourceID.createNew())
        .noticeNumber(gdpMessage.nav())
        .amount(BigDecimal.valueOf(gdpMessage.amount()))
        .description(gdpMessage.description())
        .expiryDate(expiryDate)
        .payerId(gdpMessage.debtor_tax_code())
        .payeeId(gdpMessage.ec_tax_code())
        .subject(gdpMessage.subject())
        .savingDateTime(savingDateTime)
        .iban(pagoPaConfigProperties.details().iban())
        .payTrxRef("ABC/124")
        .flgConf("flgConf")
        .serviceProviderCreditor(pagoPaConfigProperties.details().fiscalCode())
        .status(RtpStatus.CREATED)
        .events(List.of(
            Event.builder()
                .timestamp(Instant.now())
                .eventDispatcher(this.gdpEventHubProperties.eventDispatcher())
                .foreignStatus(gdpMessage.status())
                .triggerEvent(RtpEvent.CREATE_RTP)
                .build()
        ))
        .operationId(gdpMessage.id())
        .eventDispatcher(this.gdpEventHubProperties.eventDispatcher())
        .build();
  }

  private LocalDate convertLongToLocalDate(Long timestamp) {
    try {
      long milliseconds = timestamp / 1000;

      LocalDate result = Instant.ofEpochMilli(milliseconds)
          .atZone(ZoneId.of("Europe/Rome"))
          .toLocalDate();

      log.debug("Converted timestamp {} to date {}", timestamp, result);
      return result;

    } catch (Exception e) {
      log.error("Error converting timestamp {} to LocalDate", timestamp, e);
      return null;
    }
  }
}
