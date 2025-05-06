package it.gov.pagopa.rtp.sender.controller.activation;

import it.gov.pagopa.rtp.sender.utils.Users;
import it.gov.pagopa.rtp.sender.configuration.ActivationPropertiesConfig;
import it.gov.pagopa.rtp.sender.configuration.SecurityConfig;
import it.gov.pagopa.rtp.sender.controller.activation.ActivationAPIControllerImpl;
import it.gov.pagopa.rtp.sender.controller.activation.ActivationDtoMapper;
import it.gov.pagopa.rtp.sender.domain.errors.PayerAlreadyExists;
import it.gov.pagopa.rtp.sender.domain.payer.ActivationID;
import it.gov.pagopa.rtp.sender.domain.payer.Payer;
import it.gov.pagopa.rtp.sender.model.generated.activate.ActivationDto;
import it.gov.pagopa.rtp.sender.model.generated.activate.ActivationReqDto;
import it.gov.pagopa.rtp.sender.model.generated.activate.ErrorsDto;
import it.gov.pagopa.rtp.sender.model.generated.activate.PayerDto;
import it.gov.pagopa.rtp.sender.repository.activation.ActivationDBRepository;
import it.gov.pagopa.rtp.sender.service.activation.ActivationPayerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static it.gov.pagopa.rtp.sender.utils.Users.SERVICE_PROVIDER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = { ActivationAPIControllerImpl.class })
@EnableConfigurationProperties(value = ActivationPropertiesConfig.class)
@Import({ SecurityConfig.class })
@DisabledInAotMode
class ActivationAPIControllerImplTest {

  @MockitoBean
  private ActivationDBRepository activationDBRepository;

  @MockitoBean
  private ActivationPayerService activationPayerService;

  @MockitoBean
  private ActivationDtoMapper activationDtoMapper;

  private WebTestClient webTestClient;

  @Autowired
  private ApplicationContext context;

  @BeforeEach
  void setup() {
    webTestClient = WebTestClient
        .bindToApplicationContext(context)
        .apply(springSecurity())
        .configureClient()
        .build();
  }

  @Test
  @Users.RtpWriter
  void testActivatePayerSuccessful() {
    Payer payer = new Payer(ActivationID.createNew(), "RTP_SP_ID", "FISCAL_CODE", Instant.now());

    when(activationPayerService.activatePayer(any(String.class), any(String.class)))
        .thenReturn(Mono.just(payer));

    webTestClient.post()
        .uri("/activations")
        .header("RequestId", UUID.randomUUID().toString())
        .header("Version", "v1")
        .bodyValue(generateActivationRequest())
        .exchange()
        .expectStatus().isCreated().expectHeader()
        .location("http://localhost:8080/" + payer.activationID().getId().toString());
  }

  @Test
  @Users.RtpWriter
  void testActivatePayerAlreadyExists() {
    when(activationPayerService.activatePayer(any(String.class),
        any(String.class)))
        .thenReturn(Mono.error(new PayerAlreadyExists()));
    webTestClient.post()
        .uri("/activations")
        .header("RequestId", UUID.randomUUID().toString())
        .header("Version", "v1")
        .bodyValue(generateActivationRequest())
        .exchange()
        .expectStatus().isEqualTo(409);
  }


  @Test
  @Users.RtpWriter
  void givenBadFiscalCode_whenActivatePayer_thenReturnBadRequest() {

    String invalidJson = """
            {
                "payer": {
                    "fiscalCode": "INVALID",
                    "rtpSpId": "FAKESP00"
                }
            }
            """;

    // When: Sending a POST request with invalid type
    webTestClient.post()
            .uri("/activations")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidJson)
            .exchange()
            // Then: Verify the response
            .expectStatus().isBadRequest()
            .expectBody(ErrorsDto.class);

    verify(activationPayerService, times(0)).activatePayer(any(String.class), any(String.class));
    verify(activationDtoMapper, times(0)).toActivationDto(any());
  }


  @Test
  @WithMockUser(value = "another", roles = Users.ACTIVATION_WRITE_ROLE)
  void authorizedUserShouldNotActivateForAnotherServiceProvider() {
    webTestClient.post()
        .uri("/activations")
        .header("RequestId", UUID.randomUUID().toString())
        .header("Version", "v1")
        .bodyValue(generateActivationRequest())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @WithMockUser
  void userWithoutEnoughPermissionShouldNotCreateNewActivation() {
    webTestClient.post()
        .uri("/activations")
        .header("RequestId", UUID.randomUUID().toString())
        .header("Version", "v1")
        .bodyValue(generateActivationRequest())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @Users.RtpSenderWriter
  void testFindActivationByPayerIdSuccess() {
    ActivationID activationID = ActivationID.createNew();

    Payer payer = new Payer(activationID, "testRtpSpId", "RSSMRA85T10A562S", Instant.now());

    PayerDto payerDto = new PayerDto().fiscalCode(payer.fiscalCode()).rtpSpId(payer.serviceProviderDebtor());

    ActivationDto activationDto = new ActivationDto();
    activationDto.setId(activationID.getId());
    activationDto.setPayer(payerDto);
    activationDto.setEffectiveActivationDate(null);

    when(activationPayerService.findPayer(payerDto.getFiscalCode()))
        .thenReturn(Mono.just(payer));
    when(activationDtoMapper.toActivationDto(payer))
        .thenReturn(activationDto);

    webTestClient.get()
        .uri("/activations/payer")
        .header("RequestId", UUID.randomUUID().toString())
        .header("Version", "v1")
        .header("PayerId", payerDto.getFiscalCode())
        .exchange()
        .expectStatus().isOk()
        .expectBody(ActivationDto.class)
        .value(dto -> {
          assert dto.getPayer().getFiscalCode().equals(payer.fiscalCode());
        });
  }

  @Test
  @Users.RtpReader
  void getActivationThrowsException() {

    webTestClient.get()
        .uri("/activations/activation/{activationId}", UUID.randomUUID().toString())
        .header("RequestId", UUID.randomUUID().toString())
        .header("Version", "v1")
        .exchange()
        .expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.error").isEqualTo("Not Found");
  }

  @Test
  @Users.RtpReader
  void getActivationsThrowsException() {

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/activations")
            .queryParam("PageNumber", 0)
            .queryParam("PageSize", 10)
            .build())
        .header("RequestId", UUID.randomUUID().toString())
        .header("Version", "v1")
        .exchange()
        .expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.error").isEqualTo("Bad Request");
  }

  @ParameterizedTest
  @ValueSource(strings = { "rssmra85t10a562s", "RSSMRA85T10A56HS" })
  @Users.RtpSenderWriter
  void givenBadFiscalCodeWhenFindActivationThen400(String badFiscalCode) {

    webTestClient.get()
        .uri("/activations/payer")
        .header("RequestId", UUID.randomUUID().toString())
        .header("Version", "v1")
        .header("PayerId", badFiscalCode)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(ErrorsDto.class)
        .value(body -> assertThat(body.getErrors()).hasSize(1));
  }

  private ActivationReqDto generateActivationRequest() {
    return new ActivationReqDto(new PayerDto("RSSMRA85T10A562S", SERVICE_PROVIDER_ID));
  }
}
