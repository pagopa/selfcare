package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.service.PartyRegistryProxyService;
import it.pagopa.selfcare.onboarding.client.model.BusinessInfoIC;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.client.model.ManagerVerification;
import it.pagopa.selfcare.onboarding.client.model.MatchInfoResult;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgManagerVerifierTest {
    @InjectMocks
    private PgManagerVerifier pgManagerVerifier;

    @Mock
    private PartyRegistryProxyService partyRegistryProxyClient;

    @Test
    void verifyManager_userIsManagerOnInfocamere() {
        String taxCode = "validTaxCode";
        String companyTaxCode = "validCompanyTaxCode";

        InstitutionInfoIC institutionInfoIC = new InstitutionInfoIC();
        BusinessInfoIC businessInfoIC = new BusinessInfoIC();
        businessInfoIC.setBusinessTaxId(companyTaxCode);
        businessInfoIC.setBusinessName("CompanyName 1");
        institutionInfoIC.setBusinesses(List.of(businessInfoIC));

        when(partyRegistryProxyClient.getInstitutionsByUserFiscalCode(taxCode)).thenReturn(institutionInfoIC);

        ManagerVerification result = pgManagerVerifier.doVerify(taxCode, companyTaxCode);

        assertNotNull(result);
        assertEquals(Origin.INFOCAMERE.getValue(), result.getOrigin());
        assertEquals("CompanyName 1", result.getCompanyName());
        assertTrue(result.isVerified());
    }

    @Test
    void verifyManager_userIsManagerOnAde() {
        String taxCode = "validTaxCode";
        String companyTaxCode = "validCompanyTaxCode";

        InstitutionInfoIC institutionInfoIC = new InstitutionInfoIC();
        institutionInfoIC.setBusinesses(Collections.emptyList());
        when(partyRegistryProxyClient.getInstitutionsByUserFiscalCode(taxCode)).thenReturn(institutionInfoIC);

        MatchInfoResult matchInfoResult = new MatchInfoResult();
        matchInfoResult.setVerificationResult(true);
        when(partyRegistryProxyClient.matchInstitutionAndUser(companyTaxCode, taxCode)).thenReturn(matchInfoResult);

        ManagerVerification result = pgManagerVerifier.doVerify(taxCode, companyTaxCode);

        assertNotNull(result);
        assertEquals(Origin.ADE.getValue(), result.getOrigin());
        assertTrue(result.isVerified());
    }

    @Test
    void verifyManager_invalidRequestException_returnsNotVerified() {
        InstitutionInfoIC institutionInfoIC = new InstitutionInfoIC();
        institutionInfoIC.setBusinesses(Collections.emptyList());
        when(partyRegistryProxyClient.getInstitutionsByUserFiscalCode(anyString())).thenReturn(institutionInfoIC);
        when(partyRegistryProxyClient.matchInstitutionAndUser(anyString(), anyString())).thenThrow(new InvalidRequestException("Invalid request"));

        ManagerVerification managerVerification = pgManagerVerifier.doVerify("tax", "company");

        assertNotNull(managerVerification);
        assertFalse(managerVerification.isVerified());
    }
}
