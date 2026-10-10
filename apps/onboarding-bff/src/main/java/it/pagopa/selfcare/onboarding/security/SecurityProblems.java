package it.pagopa.selfcare.onboarding.security;

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/** problem+json answers produced by the security gate, with the body of the Spring BFF. */
final class SecurityProblems {

    static final String PROBLEM_JSON = "application/problem+json";
    static final String BEARER_PREFIX = "Bearer ";

    private static final String UNAUTHENTICATED_DETAIL = "An Authentication object was not found in the SecurityContext";
    private static final String INVALID_TENANT_DETAIL = "Invalid tenant context";
    private static final String UNKNOWN_ISSUER_DETAIL = "Unknown issuer";

    private SecurityProblems() {
    }

    static boolean hasBearerToken(String authorization) {
        return authorization != null && !authorization.isBlank() && authorization.startsWith(BEARER_PREFIX);
    }

    static Response unauthorized(String path) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"selfcare\"")
                .type(PROBLEM_JSON)
                .entity(body(Response.Status.UNAUTHORIZED, UNAUTHENTICATED_DETAIL, path))
                .build();
    }

    static Response unknownIssuer(String path) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(PROBLEM_JSON)
                .entity(body(Response.Status.INTERNAL_SERVER_ERROR, UNKNOWN_ISSUER_DETAIL, path))
                .build();
    }

    static Response invalidTenantContext(String path) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(PROBLEM_JSON)
                .entity(body(Response.Status.BAD_REQUEST, INVALID_TENANT_DETAIL, path))
                .build();
    }

    private static String body(Response.Status status, String detail, String path) {
        return new JsonObject()
                .put("title", status.getReasonPhrase())
                .put("status", status.getStatusCode())
                .put("detail", detail)
                .put("instance", path)
                .encode();
    }
}
