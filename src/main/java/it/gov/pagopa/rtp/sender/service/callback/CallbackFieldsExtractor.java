package it.gov.pagopa.rtp.sender.service.callback;

import com.fasterxml.jackson.databind.JsonNode;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.TransactionStatus;
import it.gov.pagopa.rtp.sender.utils.IdentifierUtils;
import it.gov.pagopa.rtp.sender.utils.JsonNodeUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Utility component responsible for extracting relevant fields from the SEPA callback JSON payload.
 *
 * <p>This class is used to extract domain-specific values such as {@link TransactionStatus}
 * and {@link ResourceID} from nested structures inside a {@link JsonNode}, following the
 * expected format of SEPA asynchronous callback messages.
 */
@Slf4j
@Component("callbackFieldsExtractor")
public class CallbackFieldsExtractor {

    /**
     * Extracts a stream of {@link TransactionStatus} values from the provided SEPA callback JSON.
     *
     * <p>This method navigates the structure of the JSON tree under:
     *
     * <pre>
     *   AsynchronousSepaRequestToPayResponse
     *     └── Document
     *         └── CdtrPmtActvtnReqStsRpt
     *             └── OrgnlPmtInfAndSts
     *                 └── TxInfAndSts
     *                     └── TxSts
     * </pre>
     *
     * <p>Each textual status is trimmed and mapped to a {@link TransactionStatus} enum. If the status
     * is unknown, it is logged and mapped to {@link TransactionStatus#ERROR}. If the structure is
     * missing, the stream fails with an {@link IllegalArgumentException}.
     *
     * @param responseNode the full callback JSON payload
     * @return a {@link Flux} of {@link TransactionStatus} values
     */
    @NonNull
    public Flux<TransactionStatus> extractTransactionStatusSend(@NonNull final JsonNode responseNode) {
        return Mono.justOrEmpty(responseNode)
                .doOnNext(node -> log.debug("Received JSON for transaction status extraction: {}", node))
                .map(node -> node.path("AsynchronousSepaRequestToPayResponse")
                        .path("Document")
                        .path("CdtrPmtActvtnReqStsRpt")
                        .path("OrgnlPmtInfAndSts"))
                .doOnNext(node -> log.debug("Navigated to OrgnlPmtInfAndSts node: {}", node))
                .filter(node -> !node.isMissingNode())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Missing field")))
                .flatMapMany(JsonNodeUtils::nodeToFlux)
                .flatMap(node -> JsonNodeUtils.nodeToFlux(node.path("TxInfAndSts")))
                .flatMap(node -> JsonNodeUtils.nodeToFlux(node.path("TxSts")))
                .map(JsonNode::asText)
                .map(StringUtils::trim)
                .doOnNext(txSt -> log.debug("Extracted raw transaction status: '{}'", txSt))
                .map(txtSt -> {
                    try {
                        TransactionStatus status = TransactionStatus.fromString(txtSt);
                        log.info("Mapped transaction status to enum: {}", status);
                        return status;
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid transaction status '{}', defaulting to ERROR", txtSt);
                        return TransactionStatus.ERROR;
                    }
                })
                .switchIfEmpty(Flux.just(TransactionStatus.ERROR));
    }

    /**
     * Extracts the {@link ResourceID} from the SEPA callback JSON payload.
     *
     * <p>This method navigates the following JSON path to locate the message ID:
     *
     * <pre>
     *   AsynchronousSepaRequestToPayResponse
     *     └── Document
     *         └── CdtrPmtActvtnReqStsRpt
     *             └── OrgnlGrpInfAndSts
     *                 └── OrgnlMsgId
     * </pre>
     *
     * <p>The extracted message ID is trimmed, validated, and converted into a {@link java.util.UUID}, then
     * wrapped in a {@link ResourceID}. If the field is missing or malformed, an {@link
     * IllegalArgumentException} is thrown.
     *
     * @param responseNode the full callback JSON payload
     * @return a {@link Mono} containing the {@link ResourceID}
     */
    @NonNull
    public Mono<ResourceID> extractResourceIDSend(@NonNull final JsonNode responseNode) {
        return Mono.justOrEmpty(responseNode)
                .doOnNext(node -> log.debug("Received JSON for resource ID extraction: {}", node))
                .map(node -> node.path("AsynchronousSepaRequestToPayResponse")
                        .path("Document")
                        .path("CdtrPmtActvtnReqStsRpt")
                        .path("OrgnlGrpInfAndSts")
                        .path("OrgnlMsgId"))
                .doOnNext(node -> log.debug("Navigated to OrgnlMsgId node: {}", node))
                .filter(node -> !node.isMissingNode())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Missing field")))
                .map(JsonNode::asText)
                .map(StringUtils::trim)
                .doOnNext(value -> log.debug("Extracted original message ID: '{}'", value))
                .map(IdentifierUtils::uuidRebuilder)
                .doOnNext(uuid -> log.debug("Rebuilt UUID: {}", uuid))
                .map(ResourceID::new)
                .doOnNext(id -> log.info("Extracted ResourceID: {}", id))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Resource id is invalid")));
    }
}

