package it.gov.pagopa.rtp.sender.integration.blobstorage;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.With;
import org.springframework.validation.annotation.Validated;

import it.gov.pagopa.rtp.sender.domain.registryfile.ServiceProvider;
import it.gov.pagopa.rtp.sender.domain.registryfile.TechnicalServiceProvider;


@With
@Validated
public record ServiceProviderDataResponse(
    @NotNull @NotEmpty List<TechnicalServiceProvider> tsps,
    @NotNull @NotEmpty List<ServiceProvider> sps
) {}