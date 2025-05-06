package it.gov.pagopa.rtp.sender.repository.activation;

import org.springframework.stereotype.Component;

import it.gov.pagopa.rtp.sender.domain.payer.ActivationID;
import it.gov.pagopa.rtp.sender.domain.payer.Payer;

@Component
public class ActivationMapper {

    public Payer toDomain(ActivationEntity activationEntity) {
        ActivationID activationID = new ActivationID((activationEntity.getId()));
        return new Payer(activationID,
                activationEntity.getServiceProviderDebtor(), activationEntity.getFiscalCode(),
                activationEntity.getEffectiveActivationDate());
    }

    public ActivationEntity toDbEntity(Payer payer) {
        return ActivationEntity.builder().id(payer.activationID().getId())
                .fiscalCode(payer.fiscalCode())
                .serviceProviderDebtor(payer.serviceProviderDebtor())
                .effectiveActivationDate(payer.effectiveActivationDate())
                .build();
    }
}
