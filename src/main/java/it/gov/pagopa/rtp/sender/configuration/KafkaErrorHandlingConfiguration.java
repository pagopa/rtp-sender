package it.gov.pagopa.rtp.sender.configuration;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@RegisterReflectionForBinding({
    org.springframework.messaging.converter.MessageConversionException.class,
    org.springframework.kafka.support.serializer.DeserializationException.class,
    com.fasterxml.jackson.core.JsonParseException.class,
    com.fasterxml.jackson.databind.exc.InvalidFormatException.class,
    com.fasterxml.jackson.databind.exc.MismatchedInputException.class
})
public class KafkaErrorHandlingConfiguration {
    @Bean
    public CommonErrorHandler kafkaErrorHandler() {

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) -> 
                    log.error("Error deserializing message. Skipping record: {}", consumerRecord, exception),
                new FixedBackOff(0L, 0L));
        
        errorHandler.addNotRetryableExceptions(
            org.springframework.messaging.converter.MessageConversionException.class,
            org.springframework.kafka.support.serializer.DeserializationException.class,
            com.fasterxml.jackson.core.JsonParseException.class,
            com.fasterxml.jackson.databind.exc.InvalidFormatException.class,
            com.fasterxml.jackson.databind.exc.MismatchedInputException.class);
        
        return errorHandler;
    }
}
