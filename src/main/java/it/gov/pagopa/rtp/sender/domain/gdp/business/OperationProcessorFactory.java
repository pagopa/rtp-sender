package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
import it.gov.pagopa.rtp.sender.service.rtp.SendRTPService;
import jakarta.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


@Component("operationProcessorFactory")
@Slf4j
public class OperationProcessorFactory {

  private static final Map<Operation, Class<? extends OperationProcessor>> PROCESSOR_MAP =
      new EnumMap<>(Operation.class) {{
    put(Operation.CREATE, CreateOperationProcessor.class);
  }};


  private final GdpMapper gdpMapper;
  private final SendRTPService sendRTPService;


  public OperationProcessorFactory(
      @NonNull final GdpMapper gdpMapper,
      @NonNull final SendRTPService sendRTPService) {

    this.gdpMapper = Objects.requireNonNull(gdpMapper);
    this.sendRTPService = Objects.requireNonNull(sendRTPService);
  }


  @NonNull
  public Mono<OperationProcessor> getProcessor(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "GdpMessage cannot be null");

    return Mono.just(gdpMessage)
        .map(GdpMessage::operation)
        .mapNotNull(PROCESSOR_MAP::get)
        .mapNotNull(this::getProcessorInstance)
        .switchIfEmpty(
            Mono.error(new UnsupportedOperationException(gdpMessage.operation().toString())));
  }


  @Nonnull
  private OperationProcessor getProcessorInstance(
      @Nonnull final Class<? extends OperationProcessor> processorClass
  ) {
    try {
      final var constructor = processorClass.getConstructor(
          this.gdpMapper.getClass(), this.sendRTPService.getClass());

      return constructor.newInstance(this.gdpMapper, this.sendRTPService);

    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
             IllegalAccessException e)
    {
      log.error(e.getLocalizedMessage(), e);
      throw new RuntimeException(e);
    }
  }

}
