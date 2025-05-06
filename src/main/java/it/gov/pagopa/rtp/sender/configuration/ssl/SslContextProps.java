package it.gov.pagopa.rtp.sender.configuration.ssl;

import jakarta.validation.constraints.NotBlank;
import lombok.With;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;


@With
@Validated
@ConfigurationProperties(prefix = "client.ssl")
public record SslContextProps(

    @NonNull
    String pfxFile,

    @DefaultValue("")
    String pfxPassword,

    @NotBlank
    String pfxType,

    @NonNull
    String jksTrustStorePath,

    @DefaultValue("")
    String jksTrustStorePassword

) {}
