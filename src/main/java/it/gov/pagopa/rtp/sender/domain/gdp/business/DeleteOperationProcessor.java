package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Processor responsible for handling GDP {@code DELETE} operations.
 * <p>
 * This processor filters GDP messages with a {@code VALID} status, retrieves the corresponding RTP using a composite key
 * (operationId and eventDispatcher), and requests cancellation of the RTP.
 * If the message is not valid, the operation is skipped.
 *
 * @see OperationProcessor
 * @see SendRTPService
 * @see GdpEventHubProperties
 */
@Slf4j
public class DeleteOperationProcessor implements OperationProcessor {

    private final SendRTPService sendRTPService;
    private final GdpEventHubProperties gdpEventHubProperties;

    /**
     * Constructs a new {@code DeleteOperationProcessor} with the given dependencies.
     *
     * @param sendRTPService        the service used to retrieve and cancel RTPs; must not be {@code null}
     * @param gdpEventHubProperties the configuration containing the event dispatcher name; must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public DeleteOperationProcessor(
            @NonNull final SendRTPService sendRTPService,
            @NonNull final GdpEventHubProperties gdpEventHubProperties) {

        this.sendRTPService = Objects.requireNonNull(sendRTPService);
        this.gdpEventHubProperties = Objects.requireNonNull(gdpEventHubProperties);
    }

    /**
     * Processes a {@link GdpMessage} of type {@code DELETE}.
     * <p>
     * If the message status is {@code VALID}, it attempts to:
     * <ul>
     *   <li>Retrieve the corresponding {@link Rtp} using the operationId and eventDispatcher</li>
     *   <li>Cancel the RTP by calling {@link SendRTPService#cancelRtp} method</li>
     * </ul>
     * If the message is not valid, it logs a warning and skips processing.
     *
     * @param gdpMessage the GDP message to process; must not be {@code null}
     * @return a {@link Mono} emitting the canceled {@link Rtp}, or an error if processing fails
     * @throws NullPointerException if {@code gdpMessage} is {@code null}
     */
    @NonNull
    @Override
    public Mono<Rtp> processOperation(@NonNull final GdpMessage gdpMessage) {
        Objects.requireNonNull(gdpMessage, "gdpMessage must not be null");

        return Mono.just(gdpMessage)
                .doFirst(() -> log.info("Processing GDP message with id {}", gdpMessage.id()))
                .flatMap(message -> sendRTPService.findRtpByCompositeKey(message.id(), this.gdpEventHubProperties.eventDispatcher()))
                .doOnNext(rtp -> log.info("Cancelling RTP with resourceID {}", rtp.resourceID()))
                .flatMap(sendRTPService::cancelRtp)
                .doOnSuccess(rtp -> log.info("Successfully processed GDP message with id: {}", gdpMessage.id()))
                .doOnError(error -> log.error("Failed to process GDP message with id {}: {}", gdpMessage.id(), error.getMessage(), error));
    }
}
