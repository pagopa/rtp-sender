package it.gov.pagopa.rtp.sender.domain.gdp;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;


@Validated
@Builder
public record GdpMessage(
    long id,
    @NotNull Operation operation,
    long timestamp,
    String iuv,
    String subject,
    String description,
    String ec_tax_code,
    String debtor_tax_code,
    String nav,
    Long due_date,
    @Positive int amount,
    Status status,
    String psp_code,
    String psp_tax_code
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