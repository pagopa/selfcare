package it.pagopa.selfcare.onboarding.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.openapi.quarkus.product_json.model.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Alternative
@ApplicationScoped
public class IntegrationProductService implements ProductService {

    private final Map<String, List<WorkflowRuleEntry>> workflowRulesMap;

    public IntegrationProductMsService() {
        this.workflowRulesMap = loadWorkflowRules();
    }

    @Override
    public Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin, ProductId productId) {
        List<WorkflowRuleEntry> rules = workflowRulesMap.get(productId.getValue());

        if (rules == null || rules.isEmpty()) {
            return Uni.createFrom().failure(new NotFoundException(
                    String.format("No workflowRules configured for Product %s", productId.getValue())));
        }

        return rules.stream()
                .filter(rule -> matches(rule.institutionType, institutionType))
                .filter(rule -> matches(rule.origin, origin))
                .findFirst()
                .map(rule -> {
                    WorkflowTypeResponse response = new WorkflowTypeResponse();
                    response.setWorkflowType(WorkflowType.valueOf(rule.workflowType));
                    return Uni.createFrom().item(response);
                })
                .orElseGet(() -> Uni.createFrom().failure(new NotFoundException(
                        String.format("No workflowRule found for product %s, institutionType %s, origin %s",
                                productId.getValue(), institutionType, origin))));
    }

  @Override
  public Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin) {
    return Uni.createFrom().item(List.of());
  }

  @Override
  public Uni<Boolean> isRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin) {
    return Uni.createFrom().item(false);
  }

  private boolean matches(String ruleValue, Enum<?> requestValue) {
        if (ruleValue == null && requestValue == null) return true;
        if (ruleValue == null || requestValue == null) return false;
        return ruleValue.equals(requestValue.name());
    }

    private Map<String, List<WorkflowRuleEntry>> loadWorkflowRules() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("integration-data/workflow-rules.json")) {
            if (is == null) {
                throw new IllegalStateException("workflow-rules.json not found in integration-data");
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load workflow-rules.json", e);
        }
    }

    private record WorkflowRuleEntry(String institutionType, String origin, String workflowType) {}
}
