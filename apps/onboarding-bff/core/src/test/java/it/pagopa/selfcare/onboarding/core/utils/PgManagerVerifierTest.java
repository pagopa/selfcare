package it.pagopa.selfcare.onboarding.core.utils;

import it.pagopa.selfcare.commons.base.utils.Origin;
import it.pagopa.selfcare.onboarding.connector.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.onboarding.connector.exceptions.InvalidRequestException;
import it.pagopa.selfcare.onboarding.connector.model.institutions.ManagerVerification;
import it.pagopa.selfcare.onboarding.connector.model.institutions.MatchInfoResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.BusinessInfoIC;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.InstitutionInfoIC;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgManagerVerifierTest {
    @InjectMocks
    private PgManagerVerifier pgManagerVerifier;

    @Mock
    private PartyRegistryProxyConnector partyRegistryProxyConnectorMock;

    @Test
    void verifyManager_userIsManagerOnInfocamere() {
        // given
        String taxCode = "validTaxCode";
        String companyTaxCode = "validCompanyTaxCode";
        InstitutionInfoIC institutionInfoIC = new InstitutionInfoIC();
        BusinessInfoIC businessInfoIC = new BusinessInfoIC();
        businessInfoIC.setBusinessTaxId("otherCompanyTaxCode");
        businessInfoIC.setBusinessName("CompanyName 2");
        BusinessInfoIC businessInfoIC2 = new BusinessInfoIC();
        businessInfoIC2.setBusinessTaxId(companyTaxCode);
        businessInfoIC2.setBusinessName("CompanyName 1");
        institutionInfoIC.setBusinesses(List.of(businessInfoIC, businessInfoIC2));
        when(partyRegistryProxyConnectorMock.getInstitutionsByUserFiscalCode(taxCode)).thenReturn(institutionInfoIC);

        // when
        ManagerVerification result = pgManagerVerifier.doVerify(taxCode, companyTaxCode);

        // then
        assertNotNull(result);
        assertEquals(Origin.INFOCAMERE.getValue(), result.getOrigin());
        assertEquals("CompanyName 1", result.getCompanyName());
    }

    @Test
    void verifyManager_userIsManagerOnAde() {
        // given
        String taxCode = "validTaxCode";
        String companyTaxCode = "validCompanyTaxCode";
        InstitutionInfoIC institutionInfoIC = new InstitutionInfoIC();
        institutionInfoIC.setBusinesses(Collections.emptyList());
        when(partyRegistryProxyConnectorMock.getInstitutionsByUserFiscalCode(taxCode)).thenReturn(institutionInfoIC);
        MatchInfoResult matchInfoResult = new MatchInfoResult();
        matchInfoResult.setVerificationResult(true);
        when(partyRegistryProxyConnectorMock.matchInstitutionAndUser(companyTaxCode, taxCode)).thenReturn(matchInfoResult);

        // when
        ManagerVerification result = pgManagerVerifier.doVerify(taxCode, companyTaxCode);

        // then
        assertNotNull(result);
        assertEquals(Origin.ADE.getValue(), result.getOrigin());
    }

    @Test
    void verifyManager_userAdeIsNull() {
        // given
        String taxCode = "validTaxCode";
        String companyTaxCode = "validCompanyTaxCode";

        //when
        InstitutionInfoIC institutionInfoIC = new InstitutionInfoIC();
        institutionInfoIC.setBusinesses(Collections.emptyList());
        when(partyRegistryProxyConnectorMock.getInstitutionsByUserFiscalCode(taxCode)).thenReturn(institutionInfoIC);
        when(partyRegistryProxyConnectorMock.matchInstitutionAndUser(companyTaxCode, taxCode)).thenReturn(null);

        // then
        ManagerVerification managerVerification = pgManagerVerifier.doVerify(taxCode, companyTaxCode);
        assertNotNull(managerVerification);
        assertFalse(managerVerification.isVerified());
    }

    @Test
    void verifyManager_userAdeIsFalse() {
        // given
        String taxCode = "validTaxCode";
        String companyTaxCode = "validCompanyTaxCode";

        //when
        InstitutionInfoIC institutionInfoIC = new InstitutionInfoIC();
        institutionInfoIC.setBusinesses(Collections.emptyList());
        when(partyRegistryProxyConnectorMock.getInstitutionsByUserFiscalCode(taxCode)).thenReturn(institutionInfoIC);
        MatchInfoResult matchInfoResult = new MatchInfoResult();
        matchInfoResult.setVerificationResult(false);
        when(partyRegistryProxyConnectorMock.matchInstitutionAndUser(companyTaxCode, taxCode)).thenReturn(matchInfoResult);

        // then
        ManagerVerification managerVerification = pgManagerVerifier.doVerify(taxCode, companyTaxCode);
        assertNotNull(managerVerification);
        assertFalse(managerVerification.isVerified());
    }

    @Test
    void verifyManager_businessNull() {
        // given
        String taxCode = "validTaxCode";
        String companyTaxCode = "validCompanyTaxCode";

        when(partyRegistryProxyConnectorMock.getInstitutionsByUserFiscalCode(anyString())).thenReturn(null);
        when(partyRegistryProxyConnectorMock.matchInstitutionAndUser(anyString(), anyString())).thenReturn(null);

        // when & then
        ManagerVerification managerVerification = pgManagerVerifier.doVerify(taxCode, companyTaxCode);
        assertNotNull(managerVerification);
        assertFalse(managerVerification.isVerified());
    }

    @Test
    void verifyManager_noBusinessFound() {
        // given
        String taxCode = "validTaxCode";
        String companyTaxCode = "validCompanyTaxCode";
        InstitutionInfoIC institutionInfoIC = new InstitutionInfoIC();
        institutionInfoIC.setBusinesses(Collections.emptyList());

        when(partyRegistryProxyConnectorMock.getInstitutionsByUserFiscalCode(anyString())).thenReturn(institutionInfoIC);
        when(partyRegistryProxyConnectorMock.matchInstitutionAndUser(anyString(), anyString())).thenReturn(null);

        // when & then
        ManagerVerification managerVerification = pgManagerVerifier.doVerify(taxCode, companyTaxCode);
        assertNotNull(managerVerification);
        assertFalse(managerVerification.isVerified());
    }

    @Test
    void verifyManager_invalidRequestException() {
        // given
        String taxCode = "validTaxCode";
        String companyTaxCode = "validCompanyTaxCode";
        InstitutionInfoIC institutionInfoIC = new InstitutionInfoIC();
        institutionInfoIC.setBusinesses(Collections.emptyList());

        when(partyRegistryProxyConnectorMock.getInstitutionsByUserFiscalCode(anyString())).thenReturn(institutionInfoIC);
        when(partyRegistryProxyConnectorMock.matchInstitutionAndUser(anyString(), anyString())).thenThrow(new InvalidRequestException("Invalid request"));

        // when & then
        ManagerVerification managerVerification = pgManagerVerifier.doVerify(taxCode, companyTaxCode);
        assertNotNull(managerVerification);
        assertFalse(managerVerification.isVerified());
    }


}