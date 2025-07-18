package it.gov.pagopa.rtp.sender.domain.rtp;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Builder;
import lombok.With;
import org.springframework.validation.annotation.Validated;

@With
@Builder
@Validated
public record Event(
    @NotNull Instant timestamp,
    String eventDispatcher,
    GdpMessage.Status foreignStatus,
    RtpStatus precStatus,
    @NotNull RtpEvent triggerEvent
) {

}
