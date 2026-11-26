package it.pagopa.selfcare.m2m;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

/** Register only on the intended downstream client, after its tenant/header propagation filter. */
@ApplicationScoped
@Priority(Priorities.USER + 100)
public class MachineTokenRequestFilter implements ResteasyReactiveClientRequestFilter {
  @Inject MachineTokenClient tokens;

  @Override
  public void filter(ResteasyReactiveClientRequestContext context) {
    if (!tokens.isEnabled() || context.getHeaderString("Authorization") != null) {
      return;
    }
    String tenant = context.getHeaderString("X-Tenant-Id");
    context.suspend();
    try {
      tokens.token(tenant).whenComplete((token, failure) -> {
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
