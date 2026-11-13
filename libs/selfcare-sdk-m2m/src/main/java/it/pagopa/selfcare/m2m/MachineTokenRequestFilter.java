package it.pagopa.selfcare.m2m;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

/** Register only on the intended downstream client, after its tenant/header propagation filter. */
@ApplicationScoped
@Priority(Priorities.USER + 100)
public class MachineTokenRequestFilter implements ResteasyReactiveClientRequestFilter {
  @Inject MachineTokenClient tokens;
  @ConfigProperty(name = "selfcare.m2m.audience") Optional<String> audience;
  @ConfigProperty(name = "selfcare.m2m.scope") Optional<String> scope;

  @Override
  public void filter(ResteasyReactiveClientRequestContext context) {
    if (!tokens.isEnabled() || context.getHeaderString("Authorization") != null) {
      return;
    }
    String tenant = context.getHeaderString("X-Tenant-Id");
    String target = audience.orElseThrow(() -> new IllegalStateException("M2M audience is required"));
    String permission = scope.orElseThrow(() -> new IllegalStateException("M2M scope is required"));
    context.suspend();
    try {
      tokens.token(tenant, target, permission).whenComplete((token, failure) -> {
        if (failure != null) {
          context.resume(failure);
        } else {
          context.getHeaders().putSingle("Authorization", "Bearer " + token);
          context.resume();
        }
      });
    } catch (RuntimeException exception) {
      context.resume(exception);
    }
  }
}
