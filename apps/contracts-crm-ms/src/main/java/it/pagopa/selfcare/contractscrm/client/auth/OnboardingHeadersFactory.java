package it.pagopa.selfcare.contractscrm.client.auth;

import it.pagopa.selfcare.contractscrm.config.ContractsCrmConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/** Adds the machine JWT and the tenant header to every onboarding-ms call. */
@ApplicationScoped
public class OnboardingHeadersFactory implements ClientHeadersFactory {

  static final String AUTHORIZATION_HEADER = "Authorization";
  static final String TENANT_HEADER = "X-Tenant-Id";

  private final MachineTokenProvider machineTokenProvider;
  private final ContractsCrmConfig config;

  public OnboardingHeadersFactory(
      MachineTokenProvider machineTokenProvider, ContractsCrmConfig config) {
    this.machineTokenProvider = machineTokenProvider;
    this.config = config;
  }

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> incomingHeaders,
      MultivaluedMap<String, String> clientOutgoingHeaders) {
    clientOutgoingHeaders.putSingle(
        AUTHORIZATION_HEADER, "Bearer " + machineTokenProvider.getToken());
    clientOutgoingHeaders.putSingle(TENANT_HEADER, config.tenantId());
    return clientOutgoingHeaders;
  }
}
