package it.pagopa.selfcare.onboarding.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryManagerADE;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.party_registry_proxy_json.model.LegalVerificationResult;

@QuarkusTest
public class RegistryManagerADETest {

    @InjectMock
    @RestClient
    NationalRegistriesApi nationalRegistriesApi;

    @Test
    void retrieveInstitution() {
        Onboarding onboarding = createOnboarding();
        RegistryManagerADE registryManagerADE = new RegistryManagerADE(onboarding, nationalRegistriesApi, "taxCode");
        LegalVerificationResult legalVerificationResult = new LegalVerificationResult();
        legalVerificationResult.setVerificationResult(true);
        when(nationalRegistriesApi.verifyLegalUsingGET(any(), any())).thenReturn(Uni.createFrom().item(legalVerificationResult));
        Boolean result = registryManagerADE.retrieveInstitution();
        assertTrue(result);
    }

    @Test
    void isNotValid() {
        Onboarding onboarding = createOnboarding();
        RegistryManagerADE registryManagerADE = new RegistryManagerADE(onboarding, nationalRegistriesApi, "taxCode");
        registryManagerADE.setResource(false);
        UniAssertSubscriber<Boolean> subscriber = registryManagerADE.isValid()
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertFailedWith(InvalidRequestException.class);
    }

    @Test
    void skipVerifyLegal() {
        Onboarding onboarding = createOnboarding();
        onboarding.setSkipVerifyLegal(true);
        RegistryManagerADE registryManagerADE = new RegistryManagerADE(onboarding, nationalRegistriesApi, "taxCode");
        registryManagerADE.setResource(true);
        Boolean result = registryManagerADE.retrieveInstitution();
        assertTrue(result);
        verifyNoInteractions(nationalRegistriesApi);
    }

    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        User user = new User();
        user.setRole(PartyRole.MANAGER);
        user.setId("idManager");
        onboarding.setUsers(List.of(user));
        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setOrigin(Origin.ADE);
        onboarding.setInstitution(institution);
        return onboarding;
    }

}
