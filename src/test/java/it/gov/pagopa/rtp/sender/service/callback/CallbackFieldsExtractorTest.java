package it.gov.pagopa.rtp.sender.service.callback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtp.sender.domain.rtp.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.stream.Stream;


class CallbackFieldsExtractorTest {

    private CallbackFieldsExtractor extractor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        extractor = new CallbackFieldsExtractor();
        objectMapper = new ObjectMapper();
    }

    @Test
    void givenValidJson_whenExtractTransactionStatusSend_thenReturnTransactionStatuses() throws Exception {
        String json = """
                    {
                      "AsynchronousSepaRequestToPayResponse": {
                        "Document": {
                          "CdtrPmtActvtnReqStsRpt": {
                            "OrgnlPmtInfAndSts": [
                              {
                                "TxInfAndSts": [
                                  {
                                    "TxSts": ["ACCP"]
                                  },
                                  {
                                    "TxSts": ["RJCT"]
                                  }
                                ]
                              }
                            ]
                          }
                        }
                      }
                    }
                """;
        JsonNode node = objectMapper.readTree(json);

        var result = extractor.extractTransactionStatusSend(node);

        StepVerifier.create(result)
                .expectNext(TransactionStatus.ACCP)
                .expectNext(TransactionStatus.RJCT)
                .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("invalidJsonPayloads")
    void givenInvalidOrUnknownTxSts_whenExtractTransactionStatusSend_thenReturnErrorStatus(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);

        var result = extractor.extractTransactionStatusSend(node);

        StepVerifier.create(result)
                .expectNext(TransactionStatus.ERROR)
                .verifyComplete();
    }

    static Stream<String> invalidJsonPayloads() {
        return Stream.of(
                // Unknown status
                """
                {
                  "AsynchronousSepaRequestToPayResponse": {
                    "Document": {
                      "CdtrPmtActvtnReqStsRpt": {
                        "OrgnlPmtInfAndSts": [
                          {
                            "TxInfAndSts": [
                              { "TxSts": ["FOO"] }
                            ]
                          }
                        ]
                      }
                    }
                  }
                }
                """,
                // Missing OrgnlPmtInfAndSts
                """
                {
                  "AsynchronousSepaRequestToPayResponse": {
                    "Document": {
                      "CdtrPmtActvtnReqStsRpt": {}
                    }
                  }
                }
                """,
                // Empty OrgnlPmtInfAndSts array
                """
                {
                  "AsynchronousSepaRequestToPayResponse": {
                    "Document": {
                      "CdtrPmtActvtnReqStsRpt": {
                        "OrgnlPmtInfAndSts": []
                      }
                    }
                  }
                }
                """,
                // Null TxSts
                """
                {
                  "AsynchronousSepaRequestToPayResponse": {
                    "Document": {
                      "CdtrPmtActvtnReqStsRpt": {
                        "OrgnlPmtInfAndSts": [
                          {
                            "TxInfAndSts": [
                              { "TxSts": [null] }
                            ]
                          }
                        ]
                      }
                    }
                  }
                }
                """,
                // Blank TxSts
                """
                {
                  "AsynchronousSepaRequestToPayResponse": {
                    "Document": {
                      "CdtrPmtActvtnReqStsRpt": {
                        "OrgnlPmtInfAndSts": [
                          {
                            "TxInfAndSts": [
                              { "TxSts": [""] }
                            ]
                          }
                        ]
                      }
                    }
                  }
                }
                """
        );
    }

    @Test
    void givenValidJson_whenExtractResourceIDSend_thenReturnResourceID() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String json = """
                    {
                      "AsynchronousSepaRequestToPayResponse": {
                        "Document": {
                          "CdtrPmtActvtnReqStsRpt": {
                            "OrgnlGrpInfAndSts": {
                              "OrgnlMsgId": "%s"
                            }
                          }
                        }
                      }
                    }
                """.formatted(uuid);
        JsonNode node = objectMapper.readTree(json);

        var result = extractor.extractResourceIDSend(node);

        StepVerifier.create(result)
                .expectNextMatches(resourceID -> resourceID.getId().toString().equals(uuid))
                .verifyComplete();
    }

    @Test
    void givenMissingMsgId_whenExtractResourceIDSend_thenThrow() throws Exception {
        String json = """
                    {
                      "AsynchronousSepaRequestToPayResponse": {
                        "Document": {
                          "CdtrPmtActvtnReqStsRpt": {
                            "OrgnlGrpInfAndSts": {}
                          }
                        }
                      }
                    }
                """;
        JsonNode node = objectMapper.readTree(json);

        var result = extractor.extractResourceIDSend(node);

        StepVerifier.create(result)
                .expectErrorMatches(e ->
                        e instanceof IllegalArgumentException &&
                                e.getMessage().equals("Missing field"))
                .verify();
    }

    @Test
    void givenInvalidUuid_whenExtractResourceIDSend_thenThrow() throws Exception {
        String json = """
                    {
                      "AsynchronousSepaRequestToPayResponse": {
                        "Document": {
                          "CdtrPmtActvtnReqStsRpt": {
                            "OrgnlGrpInfAndSts": {
                              "OrgnlMsgId": "not-a-uuid"
                            }
                          }
                        }
                      }
                    }
                """;
        JsonNode node = objectMapper.readTree(json);

        var result = extractor.extractResourceIDSend(node);

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}