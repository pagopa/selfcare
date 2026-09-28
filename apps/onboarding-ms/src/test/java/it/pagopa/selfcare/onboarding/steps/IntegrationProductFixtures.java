package it.pagopa.selfcare.onboarding.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.openapi.quarkus.product_json.model.ProductResponse;

/** Converts the historical regression catalog without relying on the removed Blob SDK. */
final class IntegrationProductFixtures {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private IntegrationProductFixtures() {}

    static Map<String, Map<String, ProductResponse>> load() {
        Map<String, Map<String, ProductResponse>> catalog = new LinkedHashMap<>();
        catalog.put("AR", new LinkedHashMap<>());
        catalog.put("PNPG", new LinkedHashMap<>());
        JsonNode workflows = read("integration-data/workflow-rules.json");
        JsonNode documentRules = read("integration-data/required-documents-rules.json");
        for (JsonNode legacy : read("integration-data/products.json")) {
            String productId = legacy.path("id").asText();
            String tenantId = "prod-pn-pg".equals(productId) ? "PNPG" : "AR";
            ObjectNode product = MAPPER.createObjectNode();
            copy(legacy, product, "parentId", "alias", "title", "description", "status",
                    "consumers", "institutionTypesAllowed", "testEnvProductIds", "signingConfiguration");
            product.put("productId", productId).put("tenantId", tenantId);
            ObjectNode features = product.putObject("features");
            copy(legacy, features, "allowCompanyOnboarding", "allowIndividualOnboarding",
                    "delegable", "invoiceable", "enabled", "expirationDays");
            ArrayNode roles = product.putArray("roleMappings");
            addRoles(roles, "DEFAULT", legacy.path("roleMappings"));
            legacy.path("roleMappingsByInstitutionType").fields()
                    .forEachRemaining(entry -> addRoles(roles, entry.getKey(), entry.getValue()));
            ArrayNode contracts = product.putArray("contracts");
            legacy.path("institutionContractMappings").fields().forEachRemaining(entry -> {
                JsonNode source = entry.getValue();
                ObjectNode contract = contracts.addObject();
                contract.put("institutionType", entry.getKey().toUpperCase(Locale.ROOT));
                contract.put("contractType", "CONTRACT");
                contract.set("path", source.path("contractTemplatePath"));
                contract.set("version", source.path("contractTemplateVersion"));
                for (JsonNode attachment : source.path("attachments")) {
                    ObjectNode target = contracts.addObject();
                    target.put("institutionType", entry.getKey().toUpperCase(Locale.ROOT));
                    target.put("contractType", "ATTACHMENT");
                    target.set("path", attachment.path("templatePath"));
                    target.set("version", attachment.path("templateVersion"));
                    copy(attachment, target, "name", "mandatory", "workflowType",
                            "workflowState", "order", "generated");
                }
            });
            if (workflows.has(productId)) {
                product.set("workflowRules", workflows.get(productId));
            }
            ArrayNode documents = product.putArray("requiredDocuments");
            for (JsonNode rule : documentRules.path(productId)) {
                if (rule.path("enabled").asBoolean()) {
                    ObjectNode document = documents.addObject();
                    document.put("id", productId + "-required-" + documents.size())
                            .put("name", "Integration required document").put("required", true);
                    ObjectNode filter = document.putObject("filter");
                    filter.putArray("institutionType").add(rule.path("institutionType").asText());
                    filter.putArray("origin").add(rule.path("origin").asText());
                }
            }
            put(catalog, product);
        }
        put(catalog, read("integration-data/product-api/prod-io-pnpg.json"));
        return catalog;
    }

    private static void addRoles(ArrayNode target, String institutionType, JsonNode source) {
        source.fields().forEachRemaining(entry -> {
            ObjectNode role = target.addObject();
            role.put("role", entry.getKey()).put("institutionType", institutionType);
            copy(entry.getValue(), role, "phasesAdditionAllowed", "skipUserCreation",
                    "excludeRoleFromUserGroups");
            if (entry.getValue().has("roles")) {
                role.set("backOfficeRoles", entry.getValue().get("roles"));
            }
        });
    }

    private static void copy(JsonNode source, ObjectNode target, String... fields) {
        for (String field : fields) {
            if (source.has(field)) {
                target.set(field, source.get(field));
            }
        }
    }

    private static void put(Map<String, Map<String, ProductResponse>> catalog, JsonNode node) {
        ProductResponse product = MAPPER.convertValue(node, ProductResponse.class);
        ProductResponse previous = catalog.get(product.getTenantId()).put(product.getProductId(), product);
        if (previous != null) {
            throw new IllegalStateException("Duplicate product fixture: " + product.getTenantId()
                    + "/" + product.getProductId());
        }
    }

    private static JsonNode read(String path) {
        try (InputStream input = IntegrationProductFixtures.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing product fixture: " + path);
            }
            return MAPPER.readTree(input);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read product fixture: " + path, e);
        }
    }
}
