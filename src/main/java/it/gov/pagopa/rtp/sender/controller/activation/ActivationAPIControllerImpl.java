package it.gov.pagopa.rtp.sender.controller.activation;

import static it.gov.pagopa.rtp.sender.utils.Authorizations.verifySubjectRequest;

import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.gov.pagopa.rtp.sender.configuration.ActivationPropertiesConfig;
import it.gov.pagopa.rtp.sender.controller.generated.activate.CreateApi;
import it.gov.pagopa.rtp.sender.controller.generated.activate.ReadApi;
import it.gov.pagopa.rtp.sender.model.generated.activate.ActivationDto;
import it.gov.pagopa.rtp.sender.model.generated.activate.ActivationReqDto;
import it.gov.pagopa.rtp.sender.model.generated.activate.PageOfActivationsDto;
import it.gov.pagopa.rtp.sender.service.activation.ActivationPayerService;
import reactor.core.publisher.Mono;

@RestController
@Validated
@Slf4j
public class ActivationAPIControllerImpl implements CreateApi, ReadApi {

  private final ActivationPayerService activationPayerService;

  private final ActivationPropertiesConfig activationPropertiesConfig;

  private final ActivationDtoMapper activationDtoMapper;

  public ActivationAPIControllerImpl(ActivationPayerService activationPayerService,
      ActivationPropertiesConfig activationPropertiesConfig,
      ActivationDtoMapper activationDtoMapper) {
    this.activationPayerService = activationPayerService;
    this.activationPropertiesConfig = activationPropertiesConfig;
    this.activationDtoMapper = activationDtoMapper;
  }

  @Override
  @PreAuthorize("hasRole('write_rtp_activations')")
  public Mono<ResponseEntity<Void>> activate(
      UUID requestId,
      String version,
      Mono<ActivationReqDto> activationReqDto,
      ServerWebExchange exchange) {
    log.info("Received request to activate a payer");
    return verifySubjectRequest(activationReqDto, it -> it.getPayer().getRtpSpId())
        .flatMap(t -> activationPayerService.activatePayer(t.getPayer().getRtpSpId(),
            t.getPayer().getFiscalCode()))
        .<ResponseEntity<Void>>map(payer -> ResponseEntity
            .created(URI.create(activationPropertiesConfig.baseUrl()
                + payer.activationID().getId().toString()))
            .build())
        .doOnError(a -> log.error("Error activating payer {}", a.getMessage()));
  }

  @Override
  @PreAuthorize("hasAnyRole('write_rtp_send','read_rtp_activations')")
  public Mono<ResponseEntity<ActivationDto>> findActivationByPayerId(
      UUID requestId,
      String payerId,
      String version,
      ServerWebExchange exchange) {
    log.info("Received request to find activation by payer id");
    return Mono.just(payerId)
        .flatMap(activationPayerService::findPayer)
        .map(activationDtoMapper::toActivationDto)
        .map(ResponseEntity::ok)
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
  }

  @Override
  @PreAuthorize("hasRole('read_rtp_activations')")
  public Mono<ResponseEntity<ActivationDto>> getActivation( UUID requestId, UUID activationId,
      String version, ServerWebExchange exchange) {
    throw new UnsupportedOperationException("Unimplemented method 'getActivation'");
  }

  @Override
  @PreAuthorize("hasRole('read_rtp_activations')")
  public Mono<ResponseEntity<PageOfActivationsDto>> getActivations(UUID requestId,
      Integer page, Integer size,
      String version, ServerWebExchange exchange) {
    throw new UnsupportedOperationException("Unimplemented method 'getActivations'");
  }

}
