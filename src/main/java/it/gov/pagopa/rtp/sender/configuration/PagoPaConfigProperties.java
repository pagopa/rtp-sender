package it.gov.pagopa.rtp.sender.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Validated
@ConfigurationProperties(prefix = "pagopa")
public record PagoPaConfigProperties(
    @NotNull Details details,
    @NotNull OperationSlug operationSlug) {

    public record Details(
        @NotBlank String iban,
        @NotBlank String fiscalCode) {}

    public record OperationSlug(
        @NotBlank String send,
        @NotBlank String cancel) {}
}
