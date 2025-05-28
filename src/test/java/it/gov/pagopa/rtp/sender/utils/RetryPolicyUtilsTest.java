package it.gov.pagopa.rtp.sender.utils;

import it.gov.pagopa.rtp.sender.configuration.ServiceProviderConfig;
import org.junit.jupiter.api.Test;
import reactor.util.retry.RetryBackoffSpec;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetryPolicyUtilsTest {

    @Test
    void givenValidRetryParams_whenSendRetryPolicy_thenReturnsRetryBackoffSpec() {
        ServiceProviderConfig.Send.Retry retryParams = mock(ServiceProviderConfig.Send.Retry.class);
        when(retryParams.maxAttempts()).thenReturn(3L);
        when(retryParams.backoffMinDuration()).thenReturn(1000L);
        when(retryParams.backoffJitter()).thenReturn(0.5d);

        RetryBackoffSpec retryPolicy = RetryPolicyUtils.sendRetryPolicy(retryParams);

        assertNotNull(retryPolicy);
        assertInstanceOf(RetryBackoffSpec.class, retryPolicy);
    }

    @Test
    void givenNullRetryParams_whenSendRetryPolicy_thenThrowsNullPointerException() {
        ServiceProviderConfig.Send.Retry retryParams = null;

        NullPointerException exception = assertThrows(NullPointerException.class, () ->
                RetryPolicyUtils.sendRetryPolicy(retryParams));
        assertEquals("Retry parameters cannot be null", exception.getMessage());
    }

}