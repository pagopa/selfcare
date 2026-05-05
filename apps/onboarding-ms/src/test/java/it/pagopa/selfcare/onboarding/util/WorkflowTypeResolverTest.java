package it.pagopa.selfcare.onboarding.util;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.util.WorkflowTypeResolver;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.SigningConfiguration;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class WorkflowTypeResolverTest {

    @Inject
    WorkflowTypeResolver workflowTypeResolver;

    @InjectMock
    ProductService productService;

    @Test
    void resolve_shouldReturnContractWithCountersignatureWhenRequiredSignaturesGreaterThanOne() {
        //given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-ced");
        onboarding.setInstitution(buildInstitution(InstitutionType.PA, Origin.IPA));
        onboarding.setIsAggregator(false);

        Product product = new Product();
        SigningConfiguration signingConfiguration = new SigningConfiguration();
        signingConfiguration.setRequiredSignatures(2);
        product.setSigningConfiguration(signingConfiguration);
        when(productService.getProductIsValid(anyString())).thenReturn(product);

        //when
        UniAssertSubscriber<WorkflowType> subscriber = workflowTypeResolver.resolve(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitItem().assertItem(WorkflowType.CONTRACT_WITH_COUNTERSIGNATURE);
    }

    @Test
    void resolve_shouldReturnContractRegistrationWhenSingleSignatureAndPaInstitution() {
        //given
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("prod-io");
        onboarding.setInstitution(buildInstitution(InstitutionType.PA, Origin.IPA));
        onboarding.setIsAggregator(false);

        Product product = new Product();
        SigningConfiguration signingConfiguration = new SigningConfiguration();
        signingConfiguration.setRequiredSignatures(1);
        product.setSigningConfiguration(signingConfiguration);
        when(productService.getProductIsValid(anyString())).thenReturn(product);

        //when
        UniAssertSubscriber<WorkflowType> subscriber = workflowTypeResolver.resolve(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        //then
        subscriber.awaitItem().assertItem(WorkflowType.CONTRACT_REGISTRATION);
    }

    private Institution buildInstitution(InstitutionType institutionType, Origin origin) {
        Institution institution = new Institution();
        institution.setInstitutionType(institutionType);
        institution.setOrigin(origin);
        return institution;
    }
}
