package it.pagopa.selfcare.onboarding.exception.handler;

import it.pagopa.selfcare.onboarding.model.error.Problem;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProblemResponses {

    public static final String APPLICATION_PROBLEM_JSON = "application/problem+json";

    // Reason phrases that differ from, or are missing in, jakarta.ws.rs.core.Response.Status
    private static final Map<Integer, String> REASON_PHRASES = Map.of(
            413, "Payload Too Large",
            414, "URI Too Long",
            416, "Requested range not satisfiable",
            422, "Unprocessable Entity");

    private ProblemResponses() {
    }

    public static Response problem(int status, String detail, UriInfo uriInfo) {
        return problem(status, detail, uriInfo, null);
    }

    public static Response problem(int status, String detail, UriInfo uriInfo, List<Problem.InvalidParam> invalidParams) {
        return problem(status, detail, instance(uriInfo), invalidParams);
    }

    public static Response problem(int status, String detail, String instance) {
        return problem(status, detail, instance, null);
    }

    public static Response problem(int status, String detail, String instance, List<Problem.InvalidParam> invalidParams) {
        Problem problem = new Problem();
        problem.setTitle(reasonPhrase(status));
        problem.setStatus(status);
        problem.setDetail(detail);
        problem.setInstance(instance);
        problem.setInvalidParams(invalidParams);
        return Response.status(status)
                .type(APPLICATION_PROBLEM_JSON)
                .entity(problem)
                .build();
    }

    public static String instance(UriInfo uriInfo) {
        return uriInfo == null || uriInfo.getRequestUri() == null ? null : uriInfo.getRequestUri().getRawPath();
    }

    public static String reasonPhrase(int status) {
        String override = REASON_PHRASES.get(status);
        if (override != null) {
            return override;
        }
        Response.Status resolved = Response.Status.fromStatusCode(status);
        return resolved == null ? null : resolved.getReasonPhrase();
    }

    public static Map<String, Object> servletError(int status, String error, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx")));
        body.put("status", status);
        body.put("error", error);
        if (path != null) {
            body.put("path", path);
        }
        return body;
    }

    /** The Spring container rejects encoded slashes before its JSON error handling is reached. */
    public static Response servletBadRequest() {
        String title = "HTTP Status 400 \u2013 Bad Request";
        return Response.status(400).type("text/html;charset=utf-8")
                .entity("<!doctype html><html lang=\"en\"><head><title>" + title + "</title>"
                        + "<style type=\"text/css\">body {font-family:Tahoma,Arial,sans-serif;} "
                        + "h1, h2, h3, b {color:white;background-color:#525D76;} h1 {font-size:22px;} "
                        + "h2 {font-size:16px;} h3 {font-size:14px;} p {font-size:12px;} "
                        + "a {color:black;} .line {height:1px;background-color:#525D76;border:none;}"
                        + "</style></head><body><h1>" + title + "</h1></body></html>")
                .build();
    }

    /** Status a downstream failure is re-exposed with; unknown and 2xx statuses become a 500. */
    public static int downstreamErrorStatus(int status) {
        boolean success = status >= 200 && status < 300;
        return !success && reasonPhrase(status) != null ? status : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }
}
