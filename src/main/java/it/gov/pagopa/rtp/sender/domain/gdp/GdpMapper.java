package it.gov.pagopa.rtp.sender.domain.gdp;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
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
import java.util.Objects;
import java.util.Optional;
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
public class GdpMapper {

  private final GdpEventHubProperties gdpEventHubProperties;


  public GdpMapper(
      @NonNull final GdpEventHubProperties gdpEventHubProperties) {

    this.gdpEventHubProperties = Objects.requireNonNull(gdpEventHubProperties);
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

    final var eventDispatcher = Optional.of(this.gdpEventHubProperties)
        .map(props ->
            props.name() + "-" + props.consumer().topic() + "-" + props.consumer().group())
        .orElseThrow(() -> new IllegalArgumentException("Couldn't create event dispatcher"));

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
        .payeeId(gdpMessage.ecTaxCode())
        .subject(gdpMessage.subject())
        .savingDateTime(savingDateTime)
        .status(RtpStatus.CREATED)
        .events(List.of(
            Event.builder()
                .timestamp(Instant.now())
                .triggerEvent(RtpEvent.CREATE_RTP)
                .build()
        ))
        .operationId(gdpMessage.id())
        .eventDispatcher(eventDispatcher)
        .build();
  }
}
