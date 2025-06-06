package it.gov.pagopa.rtp.sender.domain.gdp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;


@Validated
public record GdpMessage(
    long id,
    @NotNull Operation operation,
    @NotNull Long timestamp,
    String iuv,
    String subject,
    String description,
    String ecTaxCode,
    String debtorTaxCode,
    String nav,
    LocalDate dueDate,
    @Positive int amount,
    Status status,
    String pspCode,
    String pspTaxCode
) {

  public enum Operation {
    CREATE,
    UPDATE,
    DELETE
  }

  public enum Status {
    VALID,
    PARTIALLY_VALID,
    PAID,
    EXPIRED,
    INVALID,
    DRAFT,
    PUBLISHED
  }

}