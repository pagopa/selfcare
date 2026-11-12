package it.pagopa.selfcare.auth.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.auth.conf.MachineTokenConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MachineTokenListenerTest {
  @Test
  void disabledListenerDoesNotRequireSecretsOrOpenServer() throws Exception {
    MachineTokenListener listener = new MachineTokenListener();
    listener.config = mock(MachineTokenConfig.class);
    listener.start(null);
    verify(listener.config, never()).certificatePem();
  }

  @Test
  void refusesMainPortAndMissingTlsConfiguration() {
    MachineTokenListener listener = new MachineTokenListener();
    listener.config = mock(MachineTokenConfig.class);
    listener.mainPort = 8080;
    when(listener.config.enabled()).thenReturn(true);
    when(listener.config.port()).thenReturn(8080);
    assertThrows(IllegalStateException.class, () -> listener.start(null));
    when(listener.config.port()).thenReturn(8082);
    when(listener.config.certificatePem()).thenReturn(Optional.empty());
    assertThrows(IllegalStateException.class, () -> listener.start(null));
    when(listener.config.certificatePem()).thenReturn(Optional.of("test-certificate"));
    when(listener.config.privateKeyPem()).thenReturn(Optional.empty());
    assertThrows(IllegalStateException.class, () -> listener.start(null));
  }
}
