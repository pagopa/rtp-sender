package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
public class DeleteOperationProcessor implements OperationProcessor {

    private final SendRTPService sendRTPService;
    private final GdpEventHubProperties gdpEventHubProperties;

    public DeleteOperationProcessor(
            @NonNull final SendRTPService sendRTPService,
            @NonNull final GdpEventHubProperties gdpEventHubProperties) {

        this.sendRTPService = Objects.requireNonNull(sendRTPService);
        this.gdpEventHubProperties = Objects.requireNonNull(gdpEventHubProperties);
    }

    @NonNull
    @Override
    public Mono<Rtp> processOperation(@NonNull final GdpMessage gdpMessage) {
        Objects.requireNonNull(gdpMessage, "gdpMessage must not be null");

        log.info("Processing GDP message with id {}", gdpMessage.id());
        return Mono.just(gdpMessage)
                .filter(message -> message.status() == GdpMessage.Status.VALID)
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.warn("Skipping GDP message with id {} due to non-VALID status: {}", gdpMessage.id(), gdpMessage.status())
                ))
                .doOnNext(message -> log.debug("GDP message with id {} is VALID. Retrieving RTP...", message.id()))
                .flatMap(message -> sendRTPService.findRtpByCompositeKey(message.id(), this.gdpEventHubProperties.eventDispatcher()))
                .doOnNext(rtp -> log.info("Cancelling RTP with resourceID {}", rtp.resourceID()))
                .flatMap(rtp -> sendRTPService.cancelRtp(rtp.resourceID()))
                .doOnSuccess(rtp -> log.info("Successfully processed GDP message with id: {}", gdpMessage.id()))
                .doOnError(error -> log.error("Failed to process GDP message with id {}: {}", gdpMessage.id(), error.getMessage(), error));
    }
}
