package it.gov.pagopa.rtp.sender.service.rtp;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtp.sender.activateClient.api.ReadApi;
import it.gov.pagopa.rtp.sender.activateClient.model.ActivationDto;
import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig;
import it.gov.pagopa.rtp.sender.domain.errors.MessageBadFormed;
import it.gov.pagopa.rtp.sender.domain.errors.PayerNotActivatedException;
import it.gov.pagopa.rtp.sender.domain.errors.RtpNotFoundException;
import it.gov.pagopa.rtp.sender.domain.rtp.ResourceID;
import it.gov.pagopa.rtp.sender.domain.rtp.Rtp;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpRepository;
import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import it.gov.pagopa.rtp.sender.epcClient.model.ActiveOrHistoricCurrencyAndAmountEPC25922V30DS02WrapperDto;
import it.gov.pagopa.rtp.sender.epcClient.model.ExternalOrganisationIdentification1CodeEPC25922V30DS022WrapperDto;
import it.gov.pagopa.rtp.sender.epcClient.model.ExternalPersonIdentification1CodeEPC25922V30DS02WrapperDto;
import it.gov.pagopa.rtp.sender.epcClient.model.ExternalServiceLevel1CodeWrapperDto;
import it.gov.pagopa.rtp.sender.epcClient.model.IBAN2007IdentifierWrapperDto;
import it.gov.pagopa.rtp.sender.epcClient.model.ISODateWrapperDto;
import it.gov.pagopa.rtp.sender.epcClient.model.Max35TextWrapperDto;
import it.gov.pagopa.rtp.sender.epcClient.model.OrganisationIdentification29EPC25922V30DS022WrapperDto;
import it.gov.pagopa.rtp.sender.epcClient.model.PersonIdentification13EPC25922V30DS02WrapperDto;
import it.gov.pagopa.rtp.sender.epcClient.model.SepaRequestToPayCancellationRequestResourceDto;
import it.gov.pagopa.rtp.sender.epcClient.model.SepaRequestToPayRequestResourceDto;
import it.gov.pagopa.rtp.sender.epcClient.model.SynchronousRequestToPayCancellationResponseDto;
import it.gov.pagopa.rtp.sender.epcClient.model.SynchronousSepaRequestToPayCreationResponseDto;
import it.gov.pagopa.rtp.sender.service.rtp.handler.SendRtpProcessor;
import it.gov.pagopa.rtp.sender.utils.LoggingUtils;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RegisterReflectionForBinding({SepaRequestToPayRequestResourceDto.class,
    PersonIdentification13EPC25922V30DS02WrapperDto.class, ISODateWrapperDto.class,
    ExternalPersonIdentification1CodeEPC25922V30DS02WrapperDto.class,
    ExternalServiceLevel1CodeWrapperDto.class,
    ActiveOrHistoricCurrencyAndAmountEPC25922V30DS02WrapperDto.class, Max35TextWrapperDto.class,
    OrganisationIdentification29EPC25922V30DS022WrapperDto.class,
    ExternalOrganisationIdentification1CodeEPC25922V30DS022WrapperDto.class,
    IBAN2007IdentifierWrapperDto.class,
    ActivationDto.class,
    SynchronousSepaRequestToPayCreationResponseDto.class,
    SepaRequestToPayCancellationRequestResourceDto.class,
    SynchronousRequestToPayCancellationResponseDto.class
})
public class SendRTPServiceImpl implements SendRTPService {

  private final SepaRequestToPayMapper sepaRequestToPayMapper;
  private final ReadApi activationApi;
  private final ObjectMapper objectMapper;
  private final ServiceProviderConfig serviceProviderConfig;
  private final RtpRepository rtpRepository;
  private final SendRtpProcessor sendRtpProcessor;

  public SendRTPServiceImpl(SepaRequestToPayMapper sepaRequestToPayMapper, ReadApi activationApi,
      ServiceProviderConfig serviceProviderConfig, RtpRepository rtpRepository,
      ObjectMapper objectMapper, SendRtpProcessor sendRtpProcessor) {
    this.sepaRequestToPayMapper = sepaRequestToPayMapper;
    this.activationApi = activationApi;
    this.serviceProviderConfig = serviceProviderConfig;
    this.rtpRepository = rtpRepository;
    this.objectMapper = objectMapper;
    this.sendRtpProcessor = sendRtpProcessor;
  }

  @NonNull
  @Override
  public Mono<Rtp> send(@NonNull final Rtp rtp) {
    Objects.requireNonNull(rtp, "Rtp cannot be null");

    final var activationData = activationApi.findActivationByPayerId(UUID.randomUUID(),
            rtp.payerId(),
            serviceProviderConfig.activation().apiVersion())
        .doFirst(() -> log.info("Finding activation data for payerId: {}", rtp.payerId()))
        .doOnSuccess(act -> log.info("Activation data found for the requested payerId"))
        .doOnError(
            error -> log.error("Error finding activation data for payerId: {}", rtp.payerId(),
                error))
        .onErrorMap(WebClientResponseException.class, this::mapActivationResponseToException);

    final var rtpToSend = activationData.map(act -> act.getPayer().getRtpSpId())
        .map(rtp::toRtpWithActivationInfo)
        .doOnSuccess(
            rtpWithActivationInfo -> log.info("Saving Rtp to be sent: {}", rtpWithActivationInfo))
        .flatMap(rtpRepository::save)
        .doOnNext(savedRtp -> LoggingUtils.logAsJson(
            () -> sepaRequestToPayMapper.toEpcRequestToPay(savedRtp), objectMapper))
        .doOnSuccess(
            rtpSaved -> log.info("Rtp to be sent saved with id: {}", rtpSaved.resourceID().getId()))
        .doOnError(
            error -> log.error("Error saving Rtp to be sent: {}", error.getMessage(), error));

    return rtpToSend.flatMap(this.sendRtpProcessor::sendRtpToServiceProviderDebtor)
        .onErrorMap(WebClientResponseException.class, this::mapExternalSendResponseToException)
        .switchIfEmpty(Mono.error(new PayerNotActivatedException()));
  }


  @NonNull
  @Override
  public Mono<Rtp> cancelRtp(@NonNull final ResourceID rtpId) {
    final var rtpToCancel = this.rtpRepository
        .findById(rtpId)
        .doFirst(() -> log.info("Retrieving RTP with id {}", rtpId.getId()))
        .switchIfEmpty(Mono.error(() -> new RtpNotFoundException(rtpId.getId())))
        .doOnSuccess(
            rtp -> log.info("RTP retrieved with id {} and status {}", rtp.resourceID().getId(),
                rtp.status()))
        .doOnError(error -> log.error("Error retrieving RTP: {}", error.getMessage(), error));

    return rtpToCancel
        .doOnError(error -> log.error(error.getMessage(), error))
        .doOnNext(rtp -> LoggingUtils.logAsJson(
            () -> sepaRequestToPayMapper.toEpcRequestToCancel(rtp), objectMapper))
        .flatMap(this.sendRtpProcessor::sendRtpCancellationToServiceProviderDebtor)
        .doOnNext(rtp -> log.debug("Setting status of RTP with id {} to {}", rtp.resourceID().getId(), RtpStatus.CANCELLED))
        .flatMap(rtpRepository::save)
        .doOnSuccess(rtpSaved -> log.info("RTP saved with id: {}", rtpSaved.resourceID().getId()))
        .doOnError(error -> log.error("Error cancel RTP: {}", error.getMessage(), error));

  }


  private Throwable mapActivationResponseToException(WebClientResponseException exception) {
    return switch (exception.getStatusCode()) {
      case NOT_FOUND -> new PayerNotActivatedException();
      case BAD_REQUEST -> new MessageBadFormed(exception.getResponseBodyAsString());
      default -> new RuntimeException("Internal Server Error");
    };
  }

  private Throwable mapExternalSendResponseToException(WebClientResponseException exception) {
    return new UnsupportedOperationException("Unsupported exception handling for epc response");
  }

}