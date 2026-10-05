package it.pagopa.selfcare.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoggingContextFilterTest {

  private static final String UID = "5096e4c6-25a1-45d5-9bdf-2fb974a7c1c8";

  @Mock CurrentIdentityAssociation identityAssociation;
  @Mock SecurityIdentity securityIdentity;
  @Mock JsonWebToken jsonWebToken;
  @Mock ContainerRequestContext requestContext;
  @Mock UriInfo uriInfo;

  private LoggingContextFilter filter;

  @BeforeEach
  void setUp() {
    filter = new LoggingContextFilter();
    filter.identityAssociation = identityAssociation;
    filter.claims = List.of("uid");
    lenient().when(requestContext.getUriInfo()).thenReturn(uriInfo);
    lenient().when(uriInfo.getPath()).thenReturn("/v1/products");
    lenient().when(requestContext.getMethod()).thenReturn("GET");
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void filter_putsUidInLoggingContext() {
    givenJwtIdentity();
    when(jsonWebToken.getClaim("uid")).thenReturn(UID);

    runFilter();

    assertEquals(UID, MDC.get("uid"));
  }

  @Test
  void filter_putsAllConfiguredClaimsInLoggingContext() {
    filter.claims = List.of("uid", "tenant_id");
    givenJwtIdentity();
    when(jsonWebToken.getClaim("uid")).thenReturn(UID);
    when(jsonWebToken.getClaim("tenant_id")).thenReturn("AR");

    runFilter();

    assertEquals(UID, MDC.get("uid"));
    assertEquals("AR", MDC.get("tenant_id"));
  }

  @Test
  void filter_doesNotPutMissingClaim() {
    givenJwtIdentity();
    when(jsonWebToken.getClaim("uid")).thenReturn(null);

    runFilter();

    assertNull(MDC.get("uid"));
  }

  @Test
  void filter_doesNotPutBlankClaim() {
    givenJwtIdentity();
    when(jsonWebToken.getClaim("uid")).thenReturn("  ");

    runFilter();

    assertNull(MDC.get("uid"));
  }

  @Test
  void filter_sanitizesLineBreaksInClaim() {
    givenJwtIdentity();
    when(jsonWebToken.getClaim("uid")).thenReturn("abc\r\nINFO forged");

    runFilter();

    assertEquals("abc__INFO forged", MDC.get("uid"));
  }

  @Test
  void filter_withoutJwt_leavesLoggingContextEmpty() {
    Principal principal = () -> "not-a-jwt";
    when(identityAssociation.getDeferredIdentity())
        .thenReturn(Uni.createFrom().item(securityIdentity));
    when(securityIdentity.getPrincipal()).thenReturn(principal);

    runFilter();

    assertNull(MDC.get("uid"));
  }

  @Test
  void filter_withAnonymousIdentity_leavesLoggingContextEmpty() {
    when(identityAssociation.getDeferredIdentity())
        .thenReturn(Uni.createFrom().item(securityIdentity));
    when(securityIdentity.isAnonymous()).thenReturn(true);

    runFilter();

    assertNull(MDC.get("uid"));
  }

  @Test
  void filter_whenIdentityResolutionFails_doesNotFailRequest() {
    when(identityAssociation.getDeferredIdentity())
        .thenReturn(Uni.createFrom().failure(new AuthenticationFailedException()));

    runFilter();

    assertNull(MDC.get("uid"));
  }

  @Test
  void filter_removesStaleValuesBeforeProcessing() {
    MDC.put("uid", "stale-uid");
    when(identityAssociation.getDeferredIdentity())
        .thenReturn(Uni.createFrom().item(securityIdentity));
    when(securityIdentity.isAnonymous()).thenReturn(true);

    runFilter();

    assertNull(MDC.get("uid"));
  }

  @Test
  void filter_skipsQuarkusInternalPaths() {
    when(uriInfo.getPath()).thenReturn("/q/health");

    runFilter();

    verifyNoInteractions(identityAssociation);
    assertNull(MDC.get("uid"));
  }

  @Test
  void responseFilter_removesClaimsFromLoggingContext() {
    MDC.put("uid", UID);

    filter.removeClaimsFromLoggingContext();

    assertNull(MDC.get("uid"));
  }

  private void givenJwtIdentity() {
    when(identityAssociation.getDeferredIdentity())
        .thenReturn(Uni.createFrom().item(securityIdentity));
    when(securityIdentity.getPrincipal()).thenReturn(jsonWebToken);
  }

  private void runFilter() {
    filter
        .addClaimsToLoggingContext(requestContext)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }
}

