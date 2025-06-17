package it.gov.pagopa.rtp.sender.domain.gdp.business;

import it.gov.pagopa.rtp.sender.domain.gdp.GdpMapper;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage;
import it.gov.pagopa.rtp.sender.domain.gdp.GdpMessage.Operation;
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


  public OperationProcessorFactory(@NonNull final GdpMapper gdpMapper) {
    this.gdpMapper = Objects.requireNonNull(gdpMapper);
  }


  @NonNull
  public Mono<OperationProcessor> getProcessor(@NonNull final GdpMessage gdpMessage) {
    Objects.requireNonNull(gdpMessage, "GdpMessage cannot be null");

    return Mono.just(gdpMessage)
        .map(GdpMessage::operation)
        .mapNotNull(PROCESSOR_MAP::get)
        .mapNotNull(processorClass -> this.getProcessorInstance(
            processorClass, this.gdpMapper))
        .switchIfEmpty(
            Mono.error(new UnsupportedOperationException(gdpMessage.operation().toString())));
  }


  @Nonnull
  private OperationProcessor getProcessorInstance(
      @Nonnull final Class<? extends OperationProcessor> processorClass,
      @Nonnull final GdpMapper gdpMapper
  ) {
    try {
      Constructor<? extends OperationProcessor> constructor = processorClass.getConstructor(gdpMapper.getClass());
      return constructor.newInstance(gdpMapper);

    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
             IllegalAccessException e)
    {
      log.error(e.getLocalizedMessage(), e);
      throw new RuntimeException(e);
    }
  }

}
