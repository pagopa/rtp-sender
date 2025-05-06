package it.gov.pagopa.rtp.sender.repository.activation;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("activations")
public class ActivationEntity {
    @Id
    private UUID id;
    private String serviceProviderDebtor;
    private Instant effectiveActivationDate;

    private String fiscalCode;
}
