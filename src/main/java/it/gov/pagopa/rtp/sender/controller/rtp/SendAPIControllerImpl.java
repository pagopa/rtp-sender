package it.gov.pagopa.rtp.sender.controller.rtp;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig;
import it.gov.pagopa.rtp.sender.controller.generated.send.RtpsApi;
import it.gov.pagopa.rtp.sender.domain.errors.PayerNotActivatedException;
import it.gov.pagopa.rtp.sender.domain.errors.RtpNotFoundException;
import it.gov.pagopa.rtp.sender.domain.errors.SepaRequestException;
import it.gov.pagopa.rtp.sender.domain.errors.ServiceProviderNotFoundException;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.model.generated.send.CreateRtpDto;
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
        .doOnSuccess(rtpSaved -> MDC.put("debtor_service_provider", rtpSaved.serviceProviderDebtor()))
        .doOnSuccess(rtpSaved -> MDC.put("creditor_service_provider", rtpSaved.serviceProviderCreditor()))
        .doOnSuccess(rtpSaved -> MDC.put("payee_name", rtpSaved.payeeName()))
        .doOnSuccess(rtpSaved -> log.info("RTP sent with id: {}", rtpSaved.resourceID().getId()))
        .<ResponseEntity<Void>>map(rtp -> ResponseEntity
            .created(URI.create(serviceProviderConfig.baseUrl() + rtp.resourceID().getId()))
            .build())
        .onErrorReturn(PayerNotActivatedException.class,
            ResponseEntity.unprocessableEntity().build())
        .onErrorReturn(ServiceProviderNotFoundException.class,
            ResponseEntity.unprocessableEntity().build())
        .onErrorReturn(SepaRequestException.class,
            ResponseEntity.unprocessableEntity().build())
        .doOnError(a -> log.error("Error creating RTP {}", a.getMessage()))
        .doFinally(f -> MDC.clear());
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
        .doOnError(a -> log.error("Error cancelling RTP {}", a.getMessage()));
  }

}
