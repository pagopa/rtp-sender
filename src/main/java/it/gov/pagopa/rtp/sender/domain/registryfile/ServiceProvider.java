package it.gov.pagopa.rtp.sender.domain.registryfile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ServiceProvider(
    @NotBlank
    @JsonProperty("id")
    String id,

    @NotBlank
    @JsonProperty("name")
    String name,

    @NotBlank
    @JsonProperty("tsp_id")
    String tspId,

    @NotBlank
    @JsonProperty("psp_tax_code")
    String pspTaxCode
) {}
