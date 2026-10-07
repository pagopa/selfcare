package it.pagopa.selfcare.onboarding.parity;

import java.util.LinkedHashMap;
import java.util.Map;

/** How the BFF under test is pointed at the controlled downstream, for both implementations. */
public final class ParityTargets {

  public static final String USER_REGISTRY_API_KEY = "parity-user-registry-key";
  public static final String ONBOARDING_FUNCTIONS_API_KEY = "parity-functions-key";

  private ParityTargets() {}

  /** Environment of the Spring reference process: the same variables the infra injects. */
  public static Map<String, String> springEnvironment(DownstreamStub stub, int port) {
    Map<String, String> env = new LinkedHashMap<>();
    env.put("B4F_ONBOARDING_SERVER_PORT", String.valueOf(port));
    env.put("JWT_TOKEN_PUBLIC_KEY", ParityJwt.publicKeyPem());
    env.put("MS_ONBOARDING_URL", stub.url(DownstreamStub.MS_ONBOARDING));
    env.put("MS_CORE_URL", stub.url(DownstreamStub.MS_CORE));
    env.put("MS_IAM_URL", stub.url(DownstreamStub.MS_IAM));
    env.put("MS_USER_URL", stub.url(DownstreamStub.MS_USER));
    env.put("MS_USER_INSTITUTION_URL", stub.url(DownstreamStub.MS_USER));
    env.put("MS_PRODUCT_URL", stub.url(DownstreamStub.MS_PRODUCT));
    env.put("MS_DOCUMENT_URL", stub.url(DownstreamStub.MS_DOCUMENT));
    env.put("ONBOARDING_FUNCTIONS_URL", stub.url(DownstreamStub.ONBOARDING_FN));
    env.put("USERVICE_PARTY_PROCESS_URL", stub.url(DownstreamStub.PARTY_PROCESS));
    env.put("USERVICE_PARTY_REGISTRY_PROXY_URL", stub.url(DownstreamStub.PARTY_REGISTRY_PROXY));
    env.put("USERVICE_USER_REGISTRY_URL", stub.url(DownstreamStub.USER_REGISTRY));
    env.put("USERVICE_USER_REGISTRY_API_KEY", USER_REGISTRY_API_KEY);
    env.put("ONBOARDING-FUNCTIONS-API-KEY", ONBOARDING_FUNCTIONS_API_KEY);
    return env;
  }

  /**
   * Quarkus configuration: the infrastructure environment names (exactly the Spring ones) plus the
   * resolved {@code rest-client.*} keys, because src/test/resources/application.properties pins
   * those keys to localhost and would otherwise win over the env based expressions of the main
   * application.properties. These overrides mean this profile alone cannot prove that
   * environment-only configuration works.
   */
  public static Map<String, String> quarkusConfig(DownstreamStub stub) {
    Map<String, String> config = new LinkedHashMap<>(springEnvironment(stub, 0));
    config.remove("B4F_ONBOARDING_SERVER_PORT");
    String[][] clients = {
      {"ms-onboarding", DownstreamStub.MS_ONBOARDING},
      {"ms-user", DownstreamStub.MS_USER},
      {"ms-user-institution", DownstreamStub.MS_USER},
      {"ms-product", DownstreamStub.MS_PRODUCT},
      {"ms-core", DownstreamStub.MS_CORE},
      {"ms-document", DownstreamStub.MS_DOCUMENT},
      {"ms-iam", DownstreamStub.MS_IAM},
      {"party-process", DownstreamStub.PARTY_PROCESS},
      {"party-registry-proxy", DownstreamStub.PARTY_REGISTRY_PROXY},
      {"user-registry", DownstreamStub.USER_REGISTRY},
      {"onboarding-functions", DownstreamStub.ONBOARDING_FN},
    };
    for (String[] client : clients) {
      config.put("rest-client." + client[0] + ".base-url", stub.url(client[1]));
    }
    config.put("rest-client.user-registry.api-key", USER_REGISTRY_API_KEY);
    config.put("rest-client.onboarding-functions.api-key", ONBOARDING_FUNCTIONS_API_KEY);
    return config;
  }
}
