package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.configuration.GdpEventHubProperties;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.repository.rtp.RtpDBRepository;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
public class DeleteOperationProcessor implements OperationProcessor {

    private final SendRTPService sendRTPService;
    private final RtpDBRepository rtpDBRepository;
    private final GdpEventHubProperties gdpEventHubProperties;

    public DeleteOperationProcessor(
            @NonNull SendRTPService sendRTPService,
            @NonNull RtpDBRepository rtpDBRepository,
            @NonNull GdpEventHubProperties gdpEventHubProperties) {

        this.sendRTPService = Objects.requireNonNull(sendRTPService);
        this.rtpDBRepository = Objects.requireNonNull(rtpDBRepository);
        this.gdpEventHubProperties = Objects.requireNonNull(gdpEventHubProperties);
    }

    @Override
    public Mono<Rtp> processOperation(GdpMessage gdpMessage) {

        return Mono.just(gdpMessage)
                .filter(message -> message.status() == GdpMessage.Status.VALID)
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.info("Skipping GDP message with id {} due to non-VALID status: {}", gdpMessage.id(), gdpMessage.status())
                ))
                .flatMap(message -> this.rtpDBRepository.findByOperationIdAndEventDispatcher(
                        message.id(), this.gdpEventHubProperties.eventDispatcher()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        String.format("RTP not found with composite key: OperationId %s and EventDispatcher %s"
                        ,gdpMessage.id(), this.gdpEventHubProperties.eventDispatcher())
                )))
                .flatMap(rtp -> sendRTPService.cancelRtp(rtp.resourceID()));



    }
}
