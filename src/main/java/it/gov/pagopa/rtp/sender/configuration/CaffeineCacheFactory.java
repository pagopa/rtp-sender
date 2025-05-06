package it.gov.pagopa.rtp.sender.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import java.time.Duration;


@Component("caffeineCacheFactory")
public class CaffeineCacheFactory {

  public Caffeine<Object, Object> createCache(long maxSize, Duration expireAfterWrite) {
    return Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterWrite(expireAfterWrite);
  }
}

