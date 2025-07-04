package it.gov.pagopa.rtp.sender.controller.callback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtp.sender.domain.errors.IncorrectCertificate;
import it.gov.pagopa.rtp.sender.domain.errors.ServiceProviderNotFoundException;
import it.gov.pagopa.rtp.sender.service.callback.CallbackHandler;
import it.gov.pagopa.rtp.sender.utils.CertificateChecker;
import it.gov.pagopa.rtp.sender.utils.PayloadInfoExtractor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;

@ExtendWith(MockitoExtension.class)
class RequestToPayUpdateControllerTest {

  @Mock private CertificateChecker certificateChecker;
  @Mock private CallbackHandler callbackHandler;

  private RequestToPayUpdateController controller;
  private MockedStatic<PayloadInfoExtractor> extractorMock;
  private JsonNode requestBody;
  private final String validCertificateSerialNumber = "123456789ABCDEF";

  @BeforeEach
  void setUp() {
    extractorMock = Mockito.mockStatic(PayloadInfoExtractor.class);
    extractorMock.when(() -> PayloadInfoExtractor.populateMdc(any(JsonNode.class)))
            .thenAnswer(invocation -> null);

    controller = new RequestToPayUpdateController(certificateChecker, callbackHandler);
    requestBody = createMockRequestBody("ABCDITMMXXX");
  }

  @AfterEach
  void tearDown() {
    extractorMock.close();
  }

  @Test
  void handleRequestToPayUpdateWithValidCertificateShouldReturnOkAndPopulateMdc() {
    when(certificateChecker.verifyRequestCertificate(any(), eq(validCertificateSerialNumber)))
            .thenReturn(Mono.just(requestBody));
    when(callbackHandler.handle(any()))
            .thenReturn(Mono.just(requestBody));

    StepVerifier.create(
                    controller.handleRequestToPayUpdate(validCertificateSerialNumber, Mono.just(requestBody))
            )
            .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
            .verifyComplete();

    extractorMock.verify(() -> PayloadInfoExtractor.populateMdc(requestBody), times(1));
  }

  @Test
  void handleRequestToPayUpdateWithInvalidCertificateShouldReturnForbiddenWithoutPopulateMdc() {
    when(certificateChecker.verifyRequestCertificate(any(), eq("INVALID")))
            .thenReturn(Mono.error(new IncorrectCertificate()));

    StepVerifier.create(
                    controller.handleRequestToPayUpdate("INVALID", Mono.just(requestBody))
            )
            .expectNextMatches(response -> response.getStatusCode() == HttpStatus.FORBIDDEN)
            .verifyComplete();

    extractorMock.verify(() -> PayloadInfoExtractor.populateMdc(any(JsonNode.class)), times(0));
    verify(callbackHandler, times(0)).handle(any());
  }

  @Test
  void handleRequestToPayUpdateWithNonExistingSpIdShouldReturnBadRequestWithoutPopulateMdc() {
    when(certificateChecker.verifyRequestCertificate(any(), eq(validCertificateSerialNumber)))
            .thenReturn(Mono.error(new ServiceProviderNotFoundException("Not found")));

    StepVerifier.create(
                    controller.handleRequestToPayUpdate(validCertificateSerialNumber, Mono.just(requestBody))
            )
            .expectNextMatches(response -> response.getStatusCode() == HttpStatus.BAD_REQUEST)
            .verifyComplete();

    extractorMock.verify(() -> PayloadInfoExtractor.populateMdc(any(JsonNode.class)), times(0));
    verify(callbackHandler, times(0)).handle(any());
  }

  @Test
  void handleRequestToPayUpdateWithOtherErrorShouldPropagateErrorWithoutPopulateMdc() {
    when(certificateChecker.verifyRequestCertificate(any(), eq(validCertificateSerialNumber)))
            .thenReturn(Mono.error(new RuntimeException("Generic error")));

    StepVerifier.create(
                    controller.handleRequestToPayUpdate(validCertificateSerialNumber, Mono.just(requestBody))
            )
            .expectError(RuntimeException.class)
            .verify();

    extractorMock.verify(() -> PayloadInfoExtractor.populateMdc(any(JsonNode.class)), times(0));
    verify(callbackHandler, times(0)).handle(any());
  }

  @Test
  void handleRequestToPayUpdateWithEmptyRequestShouldReturnBadRequestWithoutPopulateMdc() {
    StepVerifier.create(
                    controller.handleRequestToPayUpdate(validCertificateSerialNumber, Mono.empty())
            )
            .expectNextMatches(response -> response.getStatusCode() == HttpStatus.BAD_REQUEST)
            .verifyComplete();

    extractorMock.verify(() -> PayloadInfoExtractor.populateMdc(any(JsonNode.class)), times(0));
    verify(callbackHandler, times(0)).handle(any());
  }

  @Test
  void handleRequestToPayUpdateWhenCallbackHandlerThrowsIllegalArgumentExceptionShouldReturnBadRequest() {
    when(certificateChecker.verifyRequestCertificate(any(), eq(validCertificateSerialNumber)))
            .thenReturn(Mono.just(requestBody));
    when(callbackHandler.handle(any()))
            .thenReturn(Mono.error(new IllegalArgumentException("Invalid payload")));

    StepVerifier.create(
                    controller.handleRequestToPayUpdate(validCertificateSerialNumber, Mono.just(requestBody))
            )
            .expectNextMatches(response -> response.getStatusCode() == HttpStatus.BAD_REQUEST)
            .verifyComplete();

    extractorMock.verify(() -> PayloadInfoExtractor.populateMdc(any(JsonNode.class)), times(0));
  }

  @Test
  void handleRequestToPayUpdateWhenCallbackHandlerThrowsServiceProviderNotFoundShouldReturnBadRequest() {
    when(certificateChecker.verifyRequestCertificate(any(), eq(validCertificateSerialNumber)))
            .thenReturn(Mono.just(requestBody));
    when(callbackHandler.handle(any()))
            .thenReturn(Mono.error(new ServiceProviderNotFoundException("SP not found")));

    StepVerifier.create(
                    controller.handleRequestToPayUpdate(validCertificateSerialNumber, Mono.just(requestBody))
            )
            .expectNextMatches(response -> response.getStatusCode() == HttpStatus.BAD_REQUEST)
            .verifyComplete();

    extractorMock.verify(() -> PayloadInfoExtractor.populateMdc(any(JsonNode.class)), times(0));
  }

  @Test
  void handleRequestToPayUpdateWhenCallbackHandlerThrowsUnexpectedErrorShouldPropagateError() {
    when(certificateChecker.verifyRequestCertificate(any(), eq(validCertificateSerialNumber)))
            .thenReturn(Mono.just(requestBody));
    when(callbackHandler.handle(any()))
            .thenReturn(Mono.error(new RuntimeException("Unexpected failure")));

    StepVerifier.create(
                    controller.handleRequestToPayUpdate(validCertificateSerialNumber, Mono.just(requestBody))
            )
            .expectError(RuntimeException.class)
            .verify();

    extractorMock.verify(() -> PayloadInfoExtractor.populateMdc(any(JsonNode.class)), times(0));
  }

  private JsonNode createMockRequestBody(String serviceProviderDebtorId) {
    final String template =
            "{\"AsynchronousSepaRequestToPayResponse\":{\"Document\":{"
                    + "\"CdtrPmtActvtnReqStsRpt\":{\"GrpHdr\":{\"InitgPty\":{"
                    + "\"Id\":{\"OrgId\":{\"AnyBIC\":\"%s\"}}}}}}}}";
    try {
      return new ObjectMapper().readTree(String.format(template, serviceProviderDebtorId));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
