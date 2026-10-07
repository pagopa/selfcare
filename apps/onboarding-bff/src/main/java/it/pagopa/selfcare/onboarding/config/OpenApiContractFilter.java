package it.pagopa.selfcare.onboarding.config;

import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Publishes the cross-cutting part of the contract the Spring BFF exposes: every operation is
 * protected by the {@code bearerAuth} scheme and declares the generic 400, 401, 404 and 500 problem
 * responses, while the endpoint-specific statuses stay declared on the controllers.
 */
public class OpenApiContractFilter implements OASFilter {

    static final String AUTH_SCHEME = "bearerAuth";
    static final String AUTH_SCOPE = "global";
    static final String PROBLEM_SCHEMA = "Problem";
    static final String PROBLEM_JSON = "application/problem+json";
    private static final String AUTO_FORBIDDEN = "Not Allowed";

    private static final Map<String, String> GLOBAL_ERRORS = Map.of(
            "400", "Bad Request",
            "401", "Unauthorized",
            "404", "Not Found",
            "500", "Internal Server Error");
    private static final Map<String, String> TAG_DESCRIPTIONS = Map.of(
            "institutions", "openapi.onboarding.institutions.api.description",
            "product", "openapi.onboarding.product.api.description",
            "user", "openapi.onboarding.user.api.description");
    private static final List<String> TAG_ORDER = List.of("institutions", "product", "user");

    @Override
    public void filterOpenAPI(OpenAPI openApi) {
        declareTags(openApi);
        declareAuthentication(openApi);
        Schema problem = problemSchema();
        openApi.getComponents().addSchema(PROBLEM_SCHEMA, problem);
        if (openApi.getPaths() != null && openApi.getPaths().getPathItems() != null) {
            openApi.getPaths().getPathItems().values().forEach(item -> operations(item).forEach(this::declareGlobals));
        }
    }

    private static List<Operation> operations(PathItem item) {
        return item.getOperations().values().stream().toList();
    }

    private void declareTags(OpenAPI openApi) {
        Config config = ConfigProvider.getConfig();
        openApi.setTags(TAG_ORDER.stream()
                .map(name -> OASFactory.createTag().name(name)
                        .description(config.getOptionalValue(TAG_DESCRIPTIONS.get(name), String.class).orElse(null)))
                .toList());
    }

    private void declareAuthentication(OpenAPI openApi) {
        String description = ConfigProvider.getConfig()
                .getOptionalValue("openapi.security.schema.bearer.description", String.class).orElse(null);
        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(description);
        if (openApi.getComponents() == null) {
            openApi.setComponents(OASFactory.createComponents());
        }
        openApi.getComponents().setSecuritySchemes(Map.of(AUTH_SCHEME, scheme));
        openApi.setSecurity(null);
    }

    private static Schema problemSchema() {
        return OASFactory.createSchema().type(List.of(Schema.SchemaType.OBJECT)).description("Generic problem response");
    }

    private void declareGlobals(Operation operation) {
        operation.setSecurity(List.of(OASFactory.createSecurityRequirement().addScheme(AUTH_SCHEME, List.of(AUTH_SCOPE))));
        if (operation.getResponses() == null) {
            operation.setResponses(OASFactory.createAPIResponses());
        }
        APIResponse forbidden = operation.getResponses().getAPIResponse("403");
        if (forbidden != null && AUTO_FORBIDDEN.equals(forbidden.getDescription())) {
            operation.getResponses().removeAPIResponse("403");
        }
        GLOBAL_ERRORS.forEach((code, reason) -> operation.getResponses().addAPIResponse(code, problemResponse(reason)));
    }

    private static APIResponse problemResponse(String reason) {
        return OASFactory.createAPIResponse()
                .description(reason)
                .content(OASFactory.createContent().addMediaType(PROBLEM_JSON,
                        OASFactory.createMediaType().schema(problemSchema())));
    }
}
