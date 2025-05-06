package it.gov.pagopa.rtp.sender.repository.activation;

import org.junit.jupiter.api.Test;

import it.gov.pagopa.rtp.sender.domain.payer.ActivationID;
import it.gov.pagopa.rtp.sender.domain.payer.Payer;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

class ActivationMapperTest {

    private ActivationMapper mapper = new ActivationMapper();

    @Test
    void testToDomain() {
        ActivationEntity activationEntity = new ActivationEntity();
        activationEntity.setId(UUID.randomUUID());
        activationEntity.setServiceProviderDebtor("RTP_SP_ID");
        activationEntity.setFiscalCode("FISCAL_CODE");
        activationEntity.setEffectiveActivationDate(Instant.ofEpochSecond(1732517304));

        Payer payer = mapper.toDomain(activationEntity);

        assertNotNull(payer);
        assertEquals(activationEntity.getId(), payer.activationID().getId());
        assertEquals(activationEntity.getServiceProviderDebtor(), payer.serviceProviderDebtor());
        assertEquals(activationEntity.getFiscalCode(), payer.fiscalCode());
        assertEquals(activationEntity.getEffectiveActivationDate(), payer.effectiveActivationDate());
    }

    @Test
    void testToDbEntity() {
        ActivationID activationID = new ActivationID(UUID.randomUUID());
        Payer payer = new Payer(activationID, "RTP_SP_ID", "FISCAL_CODE", Instant.ofEpochSecond(1732517304));

        ActivationEntity activationEntity = mapper.toDbEntity(payer);

        assertNotNull(activationEntity);
        assertEquals(payer.activationID().getId(), activationEntity.getId());
        assertEquals(payer.serviceProviderDebtor(), activationEntity.getServiceProviderDebtor());
        assertEquals(payer.fiscalCode(), activationEntity.getFiscalCode());
        assertEquals(payer.effectiveActivationDate(), activationEntity.getEffectiveActivationDate());
    }
}
