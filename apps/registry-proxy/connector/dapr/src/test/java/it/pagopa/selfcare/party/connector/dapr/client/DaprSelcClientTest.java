package it.pagopa.selfcare.party.connector.dapr.client;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class DaprSelcClientTest {

  @Mock
  DaprSelcClient daprSelcClient;

  @BeforeEach
  public void init() {
    daprSelcClient = new DaprSelcClient("x", "22", "33");
  }

  @Test
  void indexOk() throws IOException {

  }
}
