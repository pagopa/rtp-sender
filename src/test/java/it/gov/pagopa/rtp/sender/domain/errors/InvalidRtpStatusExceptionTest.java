package it.gov.pagopa.rtp.sender.domain.errors;

import it.gov.pagopa.rtp.sender.domain.rtp.RtpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvalidRtpStatusExceptionTest {

    @Test
    void shouldCreateExceptionWithCorrectMessage() {
        UUID rtpId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        RtpStatus status = RtpStatus.PAYED;

        InvalidRtpStatusException exception = new InvalidRtpStatusException(rtpId, status);

        String expectedMessage = "Cannot transition RTP with id 123e4567-e89b-12d3-a456-426614174000 from status PAYED";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void shouldBeRuntimeExceptionSubclass() {
        InvalidRtpStatusException exception = new InvalidRtpStatusException(UUID.randomUUID(), RtpStatus.CREATED);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void shouldUseHttpUnprocessableEntityAnnotation() {
        ResponseStatus annotation = InvalidRtpStatusException.class.getAnnotation(ResponseStatus.class);
        assertNotNull(annotation);
        assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, annotation.value());
    }
}
