package it.pagopa.selfcare.contractscrm.config;

import io.smallrye.config.ConfigMapping;
import java.time.Duration;
import java.util.Optional;

@ConfigMapping(prefix = "contracts-crm")
public interface ContractsCrmConfig {

  /** Tenant used for onboarding-ms calls and webhook notifications. */
  String tenantId();

  /** Topic of the Event Hub and of the webhook notifications produced by this service. */
  String topic();

  /** Onboarding status that identifies an active PSP contract. */
  String onboardingStatus();

  boolean webhookEnabled();

  MachineToken machineToken();

  interface MachineToken {
    String issuer();

    String uid();

    Duration duration();

    Optional<String> privateKey();

    Optional<String> kid();
  }
}
