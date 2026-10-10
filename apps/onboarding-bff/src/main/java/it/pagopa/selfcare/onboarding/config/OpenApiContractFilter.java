package it.pagopa.selfcare.onboarding.config;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import io.smallrye.openapi.model.BaseModel;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Publishes the contract the Spring BFF exposes: every operation is protected by the
 * {@code bearerAuth} scheme and declares the generic 400, 401, 404 and 500 problem responses, while
 * the endpoint-specific statuses stay declared on the controllers.
 *
 * <p>It also renders the specification the way springdoc did: the {@code ${...}} placeholders of
 * the annotations are resolved from the configuration, the constraints SmallRye derives from
 * {@code @NotBlank} and {@code @NotEmpty} are not published (they are still enforced at runtime),
 * the path Spring declares with a trailing slash keeps it, enums, UUIDs and dates are declared inline
 * instead of as named schemas, the responses are sorted and the generated server is the Spring one, so
 * the document does not depend on the build machine nor on the scan order.
 */
public class OpenApiContractFilter implements OASFilter {

    static final String AUTH_SCHEME = "bearerAuth";
    static final String AUTH_SCOPE = "global";
    static final String PROBLEM_SCHEMA = "Problem";
    static final String PROBLEM_JSON = "application/problem+json";
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
    private static final String SPRING_SERVER_URL = "http://localhost";
    private static final String SPRING_SERVER_DESCRIPTION = "Generated server url";
    private static final String AUTO_FORBIDDEN = "Not Allowed";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    private static final String NOT_BLANK_PATTERN = "\\S";
    private static final int NOT_EMPTY_MIN_ITEMS = 1;
    private static final Set<String> TRAILING_SLASH_PATHS = Set.of("/v1/institutions/from-infocamere");

    private static final Map<String, String> GLOBAL_ERRORS = Map.of(
            "400", "Bad Request",
            "401", "Unauthorized",
            "404", "Not Found",
            "500", "Internal Server Error");
    private static final Comparator<String> RESPONSE_ORDER =
            Comparator.comparing((String code) -> "default".equals(code)).thenComparing(Comparator.naturalOrder());

    private static final Map<String, String> TAG_DESCRIPTIONS = Map.of(
            "institutions", "openapi.onboarding.institutions.api.description",
            "product", "openapi.onboarding.product.api.description",
            "user", "openapi.onboarding.user.api.description");
    private static final List<String> TAG_ORDER = List.of("institutions", "product", "user");

    @Override
    public void filterOpenAPI(OpenAPI openApi) {
        declareServers(openApi);
        declareTags(openApi);
        declareAuthentication(openApi);
        Schema problem = problemSchema();
        openApi.getComponents().addSchema(PROBLEM_SCHEMA, problem);
        if (openApi.getPaths() != null && openApi.getPaths().getPathItems() != null) {
            openApi.getPaths().getPathItems().values().forEach(item -> operations(item).forEach(this::declareGlobals));
            keepTrailingSlashes(openApi);
        }
        inlineLeafSchemas(openApi);
    }

    @Override
    public PathItem filterPathItem(PathItem item) {
        item.setSummary(expand(item.getSummary()));
        item.setDescription(expand(item.getDescription()));
        return item;
    }

    @Override
    public Operation filterOperation(Operation operation) {
        operation.setSummary(expand(operation.getSummary()));
        operation.setDescription(expand(operation.getDescription()));
        return operation;
    }

    @Override
    public Parameter filterParameter(Parameter parameter) {
        parameter.setDescription(expand(parameter.getDescription()));
        if (parameter.getRequired() == null && parameter.getIn() != Parameter.In.PATH) {
            parameter.setRequired(Boolean.FALSE);
        }
        if (isDefaultStyle(parameter)) {
            parameter.setStyle(null);
        }
        return parameter;
    }

    // springdoc declares the optional parameters explicitly and never repeats the default serialization
    private static boolean isDefaultStyle(Parameter parameter) {
        if (parameter.getStyle() == null || parameter.getIn() == null) {
            return false;
        }
        return switch (parameter.getIn()) {
            case QUERY, COOKIE -> parameter.getStyle() == Parameter.Style.FORM;
            case PATH, HEADER -> parameter.getStyle() == Parameter.Style.SIMPLE;
        };
    }

    @Override
    public RequestBody filterRequestBody(RequestBody body) {
        body.setDescription(expand(body.getDescription()));
        if (Boolean.FALSE.equals(declaredRequired(body))) {
            body.setRequired(null);
        }
        return body;
    }

    // served at runtime, the body is re-read from the static document: `required` is a plain property that getRequired() does not expose
    private static Boolean declaredRequired(RequestBody body) {
        if (body instanceof BaseModel<?> model && model.getAllProperties().get("required") instanceof Boolean declared) {
            return declared;
        }
        return body.getRequired();
    }

    @Override
    public APIResponse filterAPIResponse(APIResponse response) {
        response.setDescription(expand(response.getDescription()));
        return response;
    }

    @Override
    public Header filterHeader(Header header) {
        header.setDescription(expand(header.getDescription()));
        return header;
    }

    @Override
    public Schema filterSchema(Schema schema) {
        schema.setTitle(expand(schema.getTitle()));
        schema.setDescription(expand(schema.getDescription()));
        if (NOT_BLANK_PATTERN.equals(schema.getPattern())) {
            schema.setPattern(null);
        }
        if (schema.getMinItems() != null && schema.getMinItems() == NOT_EMPTY_MIN_ITEMS) {
            schema.setMinItems(null);
        }
        return schema;
    }

    private static String expand(String text) {
        if (text == null || !text.contains("${")) {
            return text;
        }
        Config config = ConfigProvider.getConfig();
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder expanded = new StringBuilder();
        while (matcher.find()) {
            String value = config.getOptionalValue(matcher.group(1), String.class).orElse(matcher.group());
            matcher.appendReplacement(expanded, Matcher.quoteReplacement(value));
        }
        return matcher.appendTail(expanded).toString();
    }

    private static void keepTrailingSlashes(OpenAPI openApi) {
        TRAILING_SLASH_PATHS.forEach(path -> {
            PathItem item = openApi.getPaths().getPathItem(path);
            if (item != null) {
                openApi.getPaths().removePathItem(path);
                openApi.getPaths().addPathItem(path + "/", item);
            }
        });
    }

    private static List<Operation> operations(PathItem item) {
        return item.getOperations().values().stream().toList();
    }

    // the Spring document is what Terraform and the frontend codegen read: same generated host for both
    private static void declareServers(OpenAPI openApi) {
        openApi.setServers(List.of(OASFactory.createServer().url(SPRING_SERVER_URL).description(SPRING_SERVER_DESCRIPTION)));
    }

    /**
     * springdoc declares enums, UUIDs and dates inline, where SmallRye registers a named schema for
     * each of them: the frontend generates different types (and different decoders) from the two
     * shapes, so the leaf schemas are inlined and no longer published as models.
     */
    private static void inlineLeafSchemas(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null || components.getSchemas() == null) {
            return;
        }
        Map<String, Schema> leaves = new LinkedHashMap<>();
        components.getSchemas().forEach((name, schema) -> {
            if (isLeaf(schema)) {
                leaves.put(SCHEMA_REF_PREFIX + name, schema);
            }
        });
        components.getSchemas().values().forEach(schema -> inline(schema, leaves));
        Stream.ofNullable(components.getParameters()).flatMap(m -> m.values().stream())
                .forEach(parameter -> inline(parameter, leaves));
        Stream.ofNullable(components.getRequestBodies()).flatMap(m -> m.values().stream())
                .forEach(body -> inline(body.getContent(), leaves));
        Stream.ofNullable(components.getResponses()).flatMap(m -> m.values().stream())
                .forEach(response -> inline(response, leaves));
        Stream.ofNullable(components.getHeaders()).flatMap(m -> m.values().stream())
                .forEach(header -> inline(header, leaves));
        if (openApi.getPaths() != null && openApi.getPaths().getPathItems() != null) {
            openApi.getPaths().getPathItems().values().forEach(item -> {
                Stream.ofNullable(item.getParameters()).flatMap(List::stream).forEach(parameter -> inline(parameter, leaves));
                operations(item).forEach(operation -> {
                    Stream.ofNullable(operation.getParameters()).flatMap(List::stream)
                            .forEach(parameter -> inline(parameter, leaves));
                    if (operation.getRequestBody() != null) {
                        inline(operation.getRequestBody().getContent(), leaves);
                    }
                    if (operation.getResponses() != null) {
                        operation.getResponses().getAPIResponses().values().forEach(response -> inline(response, leaves));
                    }
                });
            });
        }
        leaves.keySet().forEach(ref -> components.removeSchema(ref.substring(SCHEMA_REF_PREFIX.length())));
    }

    private static boolean isLeaf(Schema schema) {
        return schema.getRef() == null
                && schema.getType() != null && schema.getType().equals(List.of(Schema.SchemaType.STRING))
                && (schema.getEnumeration() != null || schema.getFormat() != null)
                && schema.getProperties() == null && schema.getItems() == null
                && schema.getAllOf() == null && schema.getAnyOf() == null && schema.getOneOf() == null;
    }

    private static void inline(Parameter parameter, Map<String, Schema> leaves) {
        inline(parameter.getSchema(), leaves);
        inline(parameter.getContent(), leaves);
    }

    private static void inline(APIResponse response, Map<String, Schema> leaves) {
        inline(response.getContent(), leaves);
        Stream.ofNullable(response.getHeaders()).flatMap(m -> m.values().stream()).forEach(header -> inline(header, leaves));
    }

    private static void inline(Header header, Map<String, Schema> leaves) {
        inline(header.getSchema(), leaves);
        inline(header.getContent(), leaves);
    }

    private static void inline(Content content, Map<String, Schema> leaves) {
        if (content != null && content.getMediaTypes() != null) {
            content.getMediaTypes().values().forEach(mediaType -> inline(mediaType.getSchema(), leaves));
        }
    }

    private static void inline(Schema schema, Map<String, Schema> leaves) {
        if (schema == null) {
            return;
        }
        Schema leaf = schema.getRef() == null ? null : leaves.get(schema.getRef());
        if (leaf != null) {
            schema.setRef(null);
            absorb(schema, leaf);
        }
        Stream.ofNullable(schema.getProperties()).flatMap(m -> m.values().stream()).forEach(child -> {
            inline(child, leaves);
            describeItems(child);
        });
        if (schema.getRequired() != null) {
            // springdoc lists the required properties alphabetically
            schema.setRequired(schema.getRequired().stream().sorted().toList());
        }
        inline(schema.getItems(), leaves);
        inline(schema.getAdditionalPropertiesSchema(), leaves);
        inline(schema.getNot(), leaves);
        Stream.of(schema.getAnyOf(), schema.getOneOf(), schema.getAllOf()).filter(Objects::nonNull)
                .flatMap(List::stream).forEach(child -> inline(child, leaves));
        List<Schema> allOf = schema.getAllOf();
        if (allOf != null && allOf.size() == 1 && isLeaf(allOf.get(0))) {
            absorb(schema, allOf.get(0));
            schema.setAllOf(null);
        } else if (schema.getRef() != null && documentsOnly(schema)) {
            // OpenAPI 3.0 ignores the siblings of a reference: springdoc publishes the bare reference
            schema.setType(null);
            schema.setDescription(null);
            schema.setTitle(null);
        }
    }

    // springdoc repeats the description of an array of simple values on its items
    private static void describeItems(Schema property) {
        Schema items = property.getItems();
        if (property.getDescription() != null && items != null && items.getRef() == null && items.getDescription() == null
                && items.getProperties() == null && items.getAllOf() == null && items.getType() != null) {
            items.setDescription(property.getDescription());
        }
    }

    private static boolean documentsOnly(Schema schema) {
        return (schema.getType() == null || schema.getType().equals(List.of(Schema.SchemaType.OBJECT)))
                && Stream.of(schema.getProperties(), schema.getItems(), schema.getEnumeration(), schema.getFormat(),
                                schema.getAnyOf(), schema.getOneOf(), schema.getNot(), schema.getAdditionalPropertiesSchema(),
                                schema.getAdditionalPropertiesBoolean(), schema.getRequired(), schema.getReadOnly(),
                                schema.getWriteOnly(), schema.getDeprecated(), schema.getDefaultValue(), schema.getExamples())
                        .allMatch(OpenApiContractFilter::absent);
    }

    private static boolean absent(Object value) {
        return value == null || (value instanceof Collection<?> c && c.isEmpty())
                || (value instanceof Map<?, ?> m && m.isEmpty());
    }

    // the pattern and the example SmallRye adds to the built-in types are not published by springdoc
    private static void absorb(Schema target, Schema leaf) {
        if (target.getType() == null) {
            target.setType(leaf.getType());
        }
        if (leaf.getEnumeration() != null) {
            target.setEnumeration(leaf.getEnumeration());
        }
        if (leaf.getFormat() != null) {
            target.setFormat(leaf.getFormat());
        }
        if (target.getDescription() == null) {
            target.setDescription(leaf.getDescription());
        }
        if (target.getTitle() == null) {
            target.setTitle(leaf.getTitle());
        }
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
        operation.getResponses().getAPIResponses().values().forEach(OpenApiContractFilter::dropUndescribedContent);
        operation.getResponses().getAPIResponses().values().forEach(OpenApiContractFilter::describeBinaryContent);
        sortResponses(operation);
    }

    // a body without any schema is not declared by springdoc: the response simply has no content
    private static void dropUndescribedContent(APIResponse response) {
        Content content = response.getContent();
        if (content == null || content.getMediaTypes() == null) {
            return;
        }
        List.copyOf(content.getMediaTypes().entrySet()).stream()
                .filter(entry -> isEmpty(entry.getValue().getSchema()))
                .forEach(entry -> content.removeMediaType(entry.getKey()));
        if (content.getMediaTypes().isEmpty()) {
            response.setContent(null);
        }
    }

    private static boolean isEmpty(Schema schema) {
        return schema != null && schema.getRef() == null && schema.getType() == null && schema.getFormat() == null
                && schema.getProperties() == null && schema.getItems() == null && schema.getEnumeration() == null
                && schema.getAllOf() == null && schema.getAnyOf() == null && schema.getOneOf() == null
                && schema.getNot() == null && schema.getDescription() == null
                && schema.getAdditionalPropertiesSchema() == null && schema.getAdditionalPropertiesBoolean() == null;
    }

    // the generated document is committed and republished: its content must not depend on the scan order
    private static void sortResponses(Operation operation) {
        APIResponses current = operation.getResponses();
        Map<String, APIResponse> sorted = new TreeMap<>(RESPONSE_ORDER);
        sorted.putAll(current.getAPIResponses());
        APIResponses ordered = OASFactory.createAPIResponses();
        ordered.setExtensions(current.getExtensions());
        sorted.forEach(ordered::addAPIResponse);
        operation.setResponses(ordered);
    }

    // springdoc publishes a downloaded body as an array of bytes
    private static void describeBinaryContent(APIResponse response) {
        if (response.getContent() == null) {
            return;
        }
        MediaType binary = response.getContent().getMediaType(OCTET_STREAM);
        if (binary != null && binary.getSchema() == null) {
            binary.setSchema(OASFactory.createSchema()
                    .type(List.of(Schema.SchemaType.ARRAY))
                    .items(OASFactory.createSchema().type(List.of(Schema.SchemaType.STRING)).format("byte")));
        }
    }

    private static APIResponse problemResponse(String reason) {
        return OASFactory.createAPIResponse()
                .description(reason)
                .content(OASFactory.createContent().addMediaType(PROBLEM_JSON,
                        OASFactory.createMediaType().schema(problemSchema())));
    }
}
