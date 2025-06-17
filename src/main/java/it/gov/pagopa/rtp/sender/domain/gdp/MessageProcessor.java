package it.gov.pagopa.rtp.sender.domain.gdp;

public interface MessageProcessor<IN, OUT> {

  OUT processMessage(IN message);

}
