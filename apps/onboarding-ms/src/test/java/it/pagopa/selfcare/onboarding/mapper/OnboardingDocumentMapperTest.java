package it.pagopa.selfcare.onboarding.mapper;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.document_json.model.OnboardingDocumentRequest;
import org.openapi.quarkus.product_json.model.ProductResponse;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OnboardingDocumentMapperTest {

    @Inject
    OnboardingDocumentMapper mapper;

    @Test
    void importPreservesExistingContractWithoutTemplateMetadata() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboarding-id");
        onboarding.setProductId("prod-io");
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        onboarding.setInstitution(institution);
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        OnboardingImportContract contract = OnboardingImportContract.builder()
                .filePath("imported/contract.pdf")
                .fileName("contract.pdf")
                .createdAt(createdAt)
                .build();

        OnboardingDocumentRequest request = mapper.toRequest(onboarding,
                new ProductResponse().productId("prod-io"), contract);

        assertEquals("onboarding-id", request.getOnboardingId());
        assertEquals("prod-io", request.getProductId());
        assertEquals("imported/contract.pdf", request.getContractFilePath());
        assertEquals("contract.pdf", request.getContractFileName());
        assertEquals(createdAt, request.getContractCreatedAt());
        assertNull(request.getTemplatePath());
        assertNull(request.getTemplateVersion());
    }
}
