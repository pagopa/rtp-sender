package it.gov.pagopa.rtp.sender.controller.rtp;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig;
import it.gov.pagopa.rtp.sender.controller.generated.send.RtpsApi;
import it.gov.pagopa.rtp.sender.domain.errors.*;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.model.generated.send.CreateRtpDto;
import it.gov.pagopa.rtp.sender.model.generated.send.RtpDto;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import it.gov.pagopa.rtp.sender.utils.TokenInfo;

import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Validated
@Slf4j
public class SendAPIControllerImpl implements RtpsApi {

  private final SendRTPService sendRTPService;

  private final RtpDtoMapper rtpDtoMapper;
  private final ServiceProviderConfig serviceProviderConfig;

  public SendAPIControllerImpl(SendRTPService sendRTPService, RtpDtoMapper rtpDtoMapper,
      ServiceProviderConfig serviceProviderConfig) {
    this.sendRTPService = sendRTPService;
    this.rtpDtoMapper = rtpDtoMapper;
    this.serviceProviderConfig = serviceProviderConfig;
  }

  @WithSpan
  @Override
  @PreAuthorize("hasRole('write_rtp_send')")
  public Mono<ResponseEntity<Void>> createRtp(Mono<CreateRtpDto> createRtpDto,
      String version, ServerWebExchange exchange) {
    log.info("Received request to create RTP");
    return createRtpDto
        .flatMap(rtpDto -> TokenInfo.getTokenSubject()
            .map(sub -> rtpDtoMapper.toRtpWithServiceProviderCreditor(rtpDto, sub)))
        .flatMap(sendRTPService::send)
        .doOnSuccess(rtpSaved -> log.info("RTP sent with id: {}", rtpSaved.resourceID().getId()))
        .doOnError(a -> log.error("Error creating RTP {}", a.getMessage()))
        .<ResponseEntity<Void>>map(rtp -> ResponseEntity
            .created(URI.create(serviceProviderConfig.baseUrl() + rtp.resourceID().getId()))
            .build())
        .onErrorReturn(ServiceProviderNotFoundException.class,
            ResponseEntity.unprocessableEntity().build())
        .onErrorReturn(SepaRequestException.class,
            ResponseEntity.unprocessableEntity().build());
  }


  @Override
  @PreAuthorize("hasRole('write_rtp_send')")
  public Mono<ResponseEntity<Void>> cancelRtp(
      UUID requestId, UUID rtpId, String version,
      ServerWebExchange exchange) {

    return Mono.just(rtpId)
        .map(ResourceID::new)
        .flatMap(sendRTPService::cancelRtp)
        .<ResponseEntity<Void>>map(rtp -> ResponseEntity
            .noContent().build())
        .onErrorReturn(RtpNotFoundException.class,
            ResponseEntity.notFound().build())
        .onErrorReturn(IllegalStateException.class,
            ResponseEntity.unprocessableEntity().build())
        .doOnError(a -> log.error("Error cancelling RTP {}", a.getMessage()));
  }

  @Override
  @PreAuthorize("hasRole('read_rtp_send')")
  public Mono<ResponseEntity<RtpDto>> findRtpById(UUID requestId, UUID rtpId,
                                                  String version, ServerWebExchange exchange) {
    log.info("Received request to find RTP by id. requestId: {}, rtpId: {}", requestId, rtpId);
    return Mono.just(rtpId)
            .doOnNext(id -> log.debug("Processing findRtpById for id: {}", id))
            .flatMap(sendRTPService::findRtp)
            .doOnNext(rtp -> log.debug("RTP retrieved from sendRTPService" ))
            .map(rtpDtoMapper::toRtpDto)
            .doOnNext(dto -> log.debug("Mapped RTP with id {} to DTO", rtpId))
            .map(ResponseEntity::ok)
            .onErrorResume(RtpNotFoundException.class, ex -> {
              log.warn("Error retrieving: {}", ex.getMessage());
              return Mono.just(ResponseEntity.notFound().build());
            })
            .doOnError(a -> log.error("Error retrieving RTP {}", a.getMessage()));
  }


  @Override
  @PreAuthorize("hasRole('read_rtp_send')")
  public Mono<ResponseEntity<Flux<RtpDto>>> findRtpByNoticeNumber(String noticeNumber, UUID requestId,
      String version, ServerWebExchange exchange) {

    final var rtpsByNoticeNumberFlux = Flux.just(noticeNumber)
        .doFirst(() -> {
          MDC.put("notice_number", noticeNumber);
          MDC.put("requestId", requestId.toString());
        })

        .doFirst(() -> log.info("Received request to find RTP by notice number"))
        .flatMap(sendRTPService::findRtpsByNoticeNumber)
        .doOnComplete(() -> log.info("RTPs retrieved by notice number"))

        .doOnNext(rtp -> log.debug("Mapping RTP to DTO" ))
        .map(rtpDtoMapper::toRtpDto)
        .doOnNext(dto -> log.debug("Mapped RTP to DTO"))

        .doOnComplete(() -> log.info("Successfully retrieved RTP by notice number"))
        .doOnError(ex -> log.error("Error retrieving RTP by notice number {}", ex.getMessage(), ex));

    return Mono.just(ResponseEntity.ok(rtpsByNoticeNumberFlux))
        .doFinally(signal -> MDC.clear());
  }

}
