package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.util.OnboardingUtils;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

@QuarkusTest
class OnboardingUtilsTest {

    @Inject
    OnboardingUtils onboardingUtils;

    @InjectMock
    @RestClient
    UoApi uoApi;

    @Test
    void getUoFromRecipientCode_shouldGetData() {
        final String recipientCode = "recipientCode";
        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIpa");

        when(uoApi.findByUnicodeUsingGET1(anyString(), any()))
                .thenReturn(Uni.createFrom().item(uoResource));

        UniAssertSubscriber<UOResource> subscriber = onboardingUtils.getUoFromRecipientCode(recipientCode)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(uoResource);
    }

    @Test
    void getUoFromRecipientCode_shouldThrowResourceNotFound() {
        final String recipientCode = "recipientCode";

        when(uoApi.findByUnicodeUsingGET1(anyString(), any()))
                .thenReturn(Uni.createFrom().failure(new WebApplicationException(Response.Status.NOT_FOUND)));

        UniAssertSubscriber<UOResource> subscriber = onboardingUtils.getUoFromRecipientCode(recipientCode)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(ResourceNotFoundException.class);
    }

    @Test
    void getValidationRecipientCodeError_shouldReturnNull() {
        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIpa");
        uoResource.setCodiceFiscaleSfe("codiceFiscaleSfe");

        UniAssertSubscriber<CustomError> subscriber = onboardingUtils.getValidationRecipientCodeError("codiceIpa", uoResource)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(null);
    }

    @Test
    void getValidationRecipientCodeError_shouldReturnNoAssociation() {
        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIpa");

        UniAssertSubscriber<CustomError> subscriber = onboardingUtils.getValidationRecipientCodeError("other", uoResource)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(CustomError.DENIED_NO_ASSOCIATION);
    }

    @Test
    void getValidationRecipientCodeError_shouldReturnNoBilling() {
        UOResource uoResource = new UOResource();
        uoResource.setCodiceIpa("codiceIpa");
        uoResource.setCodiceFiscaleSfe(null);

        UniAssertSubscriber<CustomError> subscriber = onboardingUtils.getValidationRecipientCodeError("codiceIpa", uoResource)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertItem(CustomError.DENIED_NO_BILLING);
    }

    @Test
    void buildUploadSignedContractRequest_shouldBuildRequest() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(UUID.randomUUID().toString());
        onboarding.setInstitution(new it.pagopa.selfcare.onboarding.entity.Institution());
        onboarding.getInstitution().setInstitutionType(it.pagopa.selfcare.onboarding.common.InstitutionType.PA);

        Product product = new Product();
        product.setId("productId");
        product.setTitle("productTitle");
        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("path");
        contractTemplate.setContractTemplateVersion("version");
        Map<String, ContractTemplate> contractMappings = new HashMap<>();
        contractMappings.put("PA", contractTemplate);
        product.setInstitutionContractMappings(contractMappings);


        File file = new File("");

        Uni<DocumentContentControllerApi.UploadSignedContractMultipartForm> result = onboardingUtils.buildUploadSignedContractRequest(
                onboarding, false, file, product, DocumentType.INSTITUTION, Collections.emptyList());

        UniAssertSubscriber<DocumentContentControllerApi.UploadSignedContractMultipartForm> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());
        DocumentContentControllerApi.UploadSignedContractMultipartForm form = subscriber.getItem();

        assertNotNull(form);
        assertFalse(form.skipSignatureVerification);
        assertEquals(file, form._file);
        assertNotNull(form.request);
        assertEquals(onboarding.getId(), form.request.getOnboardingId());
    }
}
