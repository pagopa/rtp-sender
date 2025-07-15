package it.gov.pagopa.rtp.sender.service.callback;

import com.fasterxml.jackson.databind.JsonNode;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpRepository;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.domain.rtp.TransactionStatus;
import it.gov.pagopa.rtp.sender.service.rtp.RtpStatusUpdater;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Component responsible for handling asynchronous callback responses from SEPA services.
 *
 * <p>This handler extracts relevant fields from the incoming callback JSON,
 * retrieves the corresponding RTP from the repository and applies status transitions.
 */
@Component("callbackHandler")
@Slf4j
public class CallbackHandler {

    private final RtpRepository rtpRepository;
    private final RtpStatusUpdater rtpStatusUpdater;
    private final CallbackFieldsExtractor callbackFieldsExtractor;

    /**
     * Constructs a new {@link CallbackHandler} instance.
     *
     * @param rtpRepository the repository to fetch and save RTP records
     * @param rtpStatusUpdater the service to apply status transitions
     * @param callbackFieldsExtractor utility to extract fields from JSON callback
     */
    public CallbackHandler(
            @NonNull RtpRepository rtpRepository,
            @NonNull RtpStatusUpdater rtpStatusUpdater,
            @NonNull CallbackFieldsExtractor callbackFieldsExtractor) {
        this.rtpRepository = Objects.requireNonNull(rtpRepository);
        this.rtpStatusUpdater = Objects.requireNonNull(rtpStatusUpdater);
        this.callbackFieldsExtractor = Objects.requireNonNull(callbackFieldsExtractor);
    }

    /**
     * Handles an incoming callback payload.
     *
     * <p>This method extracts the {@link TransactionStatus} and {@link it.gov.pagopa.rtp.sender.domain.rtp.ResourceID} from the given JSON request body, retrieves
     * the associated {@link Rtp} entity, and applies the appropriate status transitions in sequence.
     * After processing, the original request body is returned.
     *
     * @param requestBody the callback payload received as a JSON object
     * @return a {@link Mono} emitting the original {@code requestBody} upon successful completion
     * @throws IllegalArgumentException or {@link IllegalStateException} in case of parsing or logic
     * errors
     */
    public Mono<JsonNode> handle(@NonNull final JsonNode requestBody) {
        final var transactionStatus = callbackFieldsExtractor.extractTransactionStatusSend(requestBody);

        return this.callbackFieldsExtractor.extractResourceIDSend(requestBody)
            .doFirst(() -> log.info("Extracting resource ID from callback request body"))
            .doOnNext(resourceID -> log.info("Attempting to retrieve RTP with resourceId: {}", resourceID.getId()))
            .flatMap(resourceID -> this.rtpRepository.findById(resourceID)
                .switchIfEmpty(
                    Mono.error(new IllegalStateException("RTP not found for resourceId " + resourceID.getId()))))
            .doOnNext(rtp -> log.info("Retrieved RTP with id {}", rtp.resourceID().getId()))
            .flatMap(rtpToUpdate -> transactionStatus
                .doOnNext(status -> log.info("Processing transaction status: {}", status))
                .concatMap(status -> triggerStatus(status, rtpToUpdate))
                .then(Mono.just(rtpToUpdate))
            )
            .doOnSuccess(r -> log.info("Completed handling callback response"))
            .thenReturn(requestBody);
    }

    /**
     * Triggers the appropriate transition logic based on the given transaction status.
     *
     * @param transactionStatus the status to handle
     * @param rtpToUpdate the RTP entity to transition
     * @return a {@link Mono} containing the updated RTP after applying the transition
     * @throws IllegalStateException if the transaction status is unsupported
     */
    @NonNull
    private Mono<Rtp> triggerStatus(@NonNull final TransactionStatus transactionStatus,
                                    @NonNull final Rtp rtpToUpdate) {

        log.debug("Handling TransactionStatus: {}", transactionStatus);

        return switch (transactionStatus) {
            case ACCP, ACWC, ACTC -> {
                log.debug("Triggering ACCEPT transition for RTP {}", rtpToUpdate.resourceID().getId());
                yield this.rtpStatusUpdater.triggerAcceptRtp(rtpToUpdate);
            }
            case RJCT -> {
                log.debug("Triggering REJECT transition for RTP {}", rtpToUpdate.resourceID().getId());
                yield Mono.just(rtpToUpdate)
                    .filter(rtp -> RtpStatus.ACCEPTED.equals(rtp.status()))
                    .flatMap(this.rtpStatusUpdater::triggerUserRejectRtp)
                    .switchIfEmpty(
                        Mono.defer(() -> this.rtpStatusUpdater.triggerRejectRtp(rtpToUpdate)));
            }
            case ERROR -> {
                log.debug("Triggering ERROR transition for RTP {}", rtpToUpdate.resourceID().getId());
                yield this.rtpStatusUpdater.triggerErrorSendRtp(rtpToUpdate)
                        .flatMap(rtp -> Mono.error(new IllegalStateException(
                                String.format("Received TransactionStatus 'ERROR' during callback processing for RTP ID: %s",
                                        rtp.resourceID().getId())
                        )));
            }
            default -> {
                log.warn("Received unsupported TransactionStatus: {}", transactionStatus);
                yield this.rtpStatusUpdater.triggerErrorSendRtp(rtpToUpdate)
                        .flatMap(rtp -> Mono.error(new IllegalStateException(
                                String.format("Unsupported TransactionStatus '%s' received for RTP ID: %s",
                                        transactionStatus, rtp.resourceID().getId())
                        )));
            }
        };
    }
}
