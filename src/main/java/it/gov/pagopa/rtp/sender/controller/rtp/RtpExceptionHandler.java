package it.gov.pagopa.rtp.sender.controller.rtp;

import io.netty.handler.timeout.ReadTimeoutException;
import it.gov.pagopa.rtp.sender.domain.errors.MessageBadFormed;
import it.gov.pagopa.rtp.sender.domain.errors.PayerNotActivatedException;
import it.gov.pagopa.rtp.sender.exception.SendErrorCode;
import it.gov.pagopa.rtp.sender.model.generated.send.ErrorDto;
import it.gov.pagopa.rtp.sender.model.generated.send.MalformedRequestErrorResponseDto;

import jakarta.validation.ConstraintViolationException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.Optional;

@RestControllerAdvice(basePackages = "it.gov.pagopa.rtp.sender.controller.rtp")
public class RtpExceptionHandler {

  private static final String MALFORMED_REQUEST_ERROR_CODE = "Malformed request";
  private static final Pattern FIELD_NAME_REGEX_PATTERN = Pattern.compile("^\\s*\\w+\\.(?<field>\\w+):.*$");


  @ExceptionHandler(MessageBadFormed.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<MalformedRequestErrorResponseDto> handleBadRequestFromActivation(
      MessageBadFormed ex) {
    var error = ex.getErrorsDto().getErrors().getFirst();
    var responseBody = new MalformedRequestErrorResponseDto()
        .error(error.getCode())
        .details(error.getDescription());
    return ResponseEntity.badRequest().body(responseBody);
  }

  @ExceptionHandler(WebExchangeBindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<MalformedRequestErrorResponseDto> handleWebExchangeBindException(
      WebExchangeBindException ex) {

    return Optional.of(ex)
        .map(WebExchangeBindException::getBindingResult)
        .map(Errors::getFieldErrors)
        .stream()
        .flatMap(Collection::stream)
        .map(error -> {
          final var errorCode = Optional.of(error)
              .map(FieldError::getCodes)
              .filter(ArrayUtils::isNotEmpty)
              .map(codes -> codes[0])
              .orElse("");

          final var description = error.getField() + " " + error.getDefaultMessage();

          return new MalformedRequestErrorResponseDto()
              .error(errorCode)
              .details(description);
        })
        .findFirst()
        .map(error -> ResponseEntity.badRequest().body(error))
        .orElse(ResponseEntity.badRequest().build());
  }


  /**
   * Handles {@link DecodingException} exceptions thrown during the processing of requests.
   * <p>
   * This method captures the exception, extracts the most specific cause of the decoding error, and
   * constructs an appropriate response containing error details. If the cause of the exception is
   * null, a fallback message ("Malformed request") is used.
   * </p>
   *
   * <p><strong>Behavior:</strong></p>
   * <ul>
   *   <li>Sets the HTTP status of the response to {@code 400 Bad Request}.</li>
   *   <li>Extracts the localized error message from the exception's most specific cause.
   *       If the cause is {@code null}, a default error code ("Malformed request") is used.</li>
   *   <li>Creates a {@link MalformedRequestErrorResponseDto} object that encapsulates
   *       the error details, including a generic error code ("Malformed request")
   *       and a description of the decoding issue.</li>
   *   <li>Returns a {@link ResponseEntity} containing the {@link MalformedRequestErrorResponseDto} as the response body.</li>
   * </ul>
   *
   * @param ex the {@link DecodingException} thrown during request decoding, typically due to
   *           malformed input. Must not be {@code null}.
   * @return a {@link ResponseEntity} with status {@code 400 Bad Request}, containing a
   * {@link MalformedRequestErrorResponseDto} object that details the error.
   */
  @ExceptionHandler(DecodingException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<MalformedRequestErrorResponseDto> handleDecodingException(
      @NonNull final DecodingException ex) {
    final var description = Optional.of(ex)
        .map(DecodingException::getMostSpecificCause)
        .map(Throwable::getLocalizedMessage)
        .orElse(MALFORMED_REQUEST_ERROR_CODE);

    final var errorsDto = new MalformedRequestErrorResponseDto()
        .error(MALFORMED_REQUEST_ERROR_CODE)
        .details(description);

    return ResponseEntity.badRequest().body(errorsDto);
  }


  /**
   * Handles {@link ConstraintViolationException} exceptions thrown upon parsing incoming requests
   * when some field in the payload doesn't abide to provided constraints.
   *
   * @param ex the {@link ConstraintViolationException} thrown upon parsing request.
   * @return a {@link ResponseEntity} with status {@code 400 Bad Request}.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorDto> handleConstraintViolationException(
      @NonNull final ConstraintViolationException ex) {

    final var errorCode = Optional.of(ex)
        .map(ConstraintViolationException::getMessage)
        .map(FIELD_NAME_REGEX_PATTERN::matcher)
        .filter(Matcher::find)
        .map(matcher -> matcher.group("field"))
        .filter(field -> field.equals("noticeNumber"))
        .map(field -> SendErrorCode.INVALID_NOTICE_NUMBER_FORMAT)
        .orElse(SendErrorCode.INVALID_REQUEST_FORMAT);

    final var errorDto = new ErrorDto()
        .code(errorCode.getCode())
        .description(errorCode.getMessage());

    return ResponseEntity.badRequest().body(errorDto);
  }


  /**
   * Handles {@link ReadTimeoutException} exceptions thrown during the processing of requests.
   * @param ex the {@link ReadTimeoutException} thrown during request processing.
   * @return a {@link ResponseEntity} with status {@code 504 Gateway Timeout}.
   */
  @ExceptionHandler(ReadTimeoutException.class)
  @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
  public ResponseEntity<Void> handleReadTimeoutException(ReadTimeoutException ex) {
    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
  }

  /**
   * Handles {@link PayerNotActivatedException} by returning a standardized error response.
   * <p>
   * This method is triggered when a request fails because the payer is not activated.
   * It returns an HTTP status {@code 422 Unprocessable Entity} along
   * with {@link SendErrorCode#PAYER_NOT_ACTIVATED} as error code.
   * </p>
   *
   * @return a {@link ResponseEntity} containing an {@link ErrorDto} with one error
   *         indicating that the payer is not activated.
   */
  @ExceptionHandler(PayerNotActivatedException.class)
  public ResponseEntity<ErrorDto> handlePayerNotActivated() {
    var error = new ErrorDto()
            .code(SendErrorCode.PAYER_NOT_ACTIVATED.getCode())
            .description(SendErrorCode.PAYER_NOT_ACTIVATED.getMessage());

    return ResponseEntity.unprocessableEntity()
        .body(error);
  }
}
