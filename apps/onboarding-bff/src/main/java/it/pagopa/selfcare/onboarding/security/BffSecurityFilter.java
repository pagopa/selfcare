package it.pagopa.selfcare.onboarding.security;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import it.pagopa.selfcare.onboarding.exception.handler.ProblemResponses;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Request gate with the same external behavior of the Spring BFF security chain: every path but the
 * documentation/health ones requires a valid JWT (401 problem+json otherwise, on unknown paths too)
 * and SPID tokens must carry the tenant announced by the {@code X-Tenant-Id} header (400 problem+json
 * otherwise).
 */
@Slf4j
@ApplicationScoped
public class BffSecurityFilter {

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @ServerRequestFilter(preMatching = true, priority = Priorities.AUTHENTICATION)
    public Uni<Response> filter(RoutingContext context) {
        String path = context.request().path();
        if (path.contains("//")) {
            return Uni.createFrom().item(Response.status(400).type("application/json")
                    .entity(ProblemResponses.servletError(400, "Bad Request", path)).build());
        }
        if (SecurityPaths.isPublic(path)) {
            return Uni.createFrom().nullItem();
        }
        responseHeaders(context);
        if (isCorsPreflight(context)) {
            return Uni.createFrom().item(Response.status(403).entity("Invalid CORS request").build());
        }
        if (!SecurityProblems.hasBearerToken(context.request().getHeader(HttpHeaders.AUTHORIZATION))) {
            return Uni.createFrom().item(SecurityProblems.unauthorized(path));
        }
        return identityAssociation.getDeferredIdentity()
                .map(identity -> check(identity, context, path))
                .onFailure().recoverWithItem(failure -> {
                    if (isUnknownIssuer(failure)) {
                        log.warn("Unknown token issuer on {}", path);
                        return SecurityProblems.unknownIssuer(path);
                    }
                    log.warn("Cannot set user authentication on {}: {}", path, causes(failure));
                    return SecurityProblems.unauthorized(path);
                });
    }

    private static boolean isUnknownIssuer(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause instanceof BffJwtCallerPrincipalFactory.UnknownIssuerException) {
                return true;
            }
        }
        return false;
    }

    // Class names only: the messages of the JWT parser can carry claim values
    private static String causes(Throwable failure) {
        StringBuilder chain = new StringBuilder();
        for (Throwable cause = failure; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (chain.length() > 0) {
                chain.append(" <- ");
            }
            chain.append(cause.getClass().getSimpleName());
        }
        return chain.toString();
    }

    private Response check(SecurityIdentity identity, RoutingContext context, String path) {
        if (identity == null || identity.isAnonymous()) {
            return SecurityProblems.unauthorized(path);
        }
        Principal principal = identity.getPrincipal();
        if (principal instanceof JsonWebToken jwt) {
            if (!TenantPolicy.isValid(jwt, context.request().getHeader(TenantPolicy.TENANT_HEADER))) {
                log.warn("Cannot validate tenant context for request {}", path);
                return SecurityProblems.invalidTenantContext(path);
            }
            if (!IdentityClaims.areStrings(jwt)) {
                log.warn("Cannot read the identity claims of the token on {}", path);
                return SecurityProblems.unauthorized(path);
            }
        }
        if ("/v1/institutions/from-infocamere".equals(path)
                || path.length() > 1 && path.endsWith("/") && !"/v1/institutions/from-infocamere/".equals(path)) {
            String resource = path.replaceAll("(^/+)|(/+$)", "");
            return ProblemResponses.problem(400, "No static resource " + resource + ".", path);
        }
        return null;
    }

    private static boolean isCorsPreflight(RoutingContext context) {
        String origin = context.request().getHeader("Origin");
        if (context.request().method() != HttpMethod.OPTIONS || origin == null
                || context.request().getHeader("Access-Control-Request-Method") == null) {
            return false;
        }
        URI caller = URI.create(origin);
        URI server = URI.create(context.request().absoluteURI());
        return !java.util.Objects.equals(caller.getScheme(), server.getScheme())
                || !java.util.Objects.equals(caller.getHost(), server.getHost())
                || port(caller) != port(server);
    }

    private static int port(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equals(uri.getScheme()) ? 443 : 80;
    }

    private static void responseHeaders(RoutingContext context) {
        context.addHeadersEndHandler(ignored -> {
            var headers = context.response().headers();
            if (!headers.contains("Cache-Control") && !headers.contains("Pragma") && !headers.contains("Expires")) {
                headers.set("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                headers.set("Pragma", "no-cache");
                headers.set("Expires", "0");
            }
            Map.of("X-Content-Type-Options", "nosniff", "X-Frame-Options", "DENY", "X-XSS-Protection", "0")
                    .forEach((name, value) -> {
                        if (!headers.contains(name)) {
                            headers.set(name, value);
                        }
                    });
            for (String vary : List.of("Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers")) {
                if (headers.getAll("Vary").stream().noneMatch(vary::equalsIgnoreCase)) {
                    headers.add("Vary", vary);
                }
            }
        });
    }
}
