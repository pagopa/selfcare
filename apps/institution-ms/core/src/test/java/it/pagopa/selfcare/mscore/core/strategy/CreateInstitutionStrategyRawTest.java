package it.pagopa.selfcare.mscore.core.strategy;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateInstitutionStrategyRawTest {

    private Institution institution;
    private CreateInstitutionStrategyRaw strategy;

    private InstitutionConnector institutionConnector;

    @BeforeEach
    void initInstitution() {
        institution = new Institution();
        institutionConnector = mock(InstitutionConnector.class);
        strategy = new CreateInstitutionStrategyRaw(institutionConnector);
        strategy.setInstitution(institution);
    }


    @Test
    void createInstitutionStrategyRawWithPassedOriginAndOriginId() {
        institution.setOrigin("CUSTOM");
        institution.setOriginId("CUSTOM_taxCode");
        institution.setIstatCode("CUSTOM_istatCode");
        institution.setLegalForm("CUSTOM_legalForm");
        when(institutionConnector.findByTaxCodeAndSubunitCode("taxCode", null, null)).thenReturn(Collections.emptyList());
        when(institutionConnector.save(institution)).thenReturn(institution);
        Institution response = strategy.createInstitution(CreateInstitutionStrategyInput.builder().taxCode("taxCode").build());
        Assertions.assertEquals("CUSTOM", response.getOrigin());
        Assertions.assertEquals("CUSTOM_taxCode", response.getOriginId());
        Assertions.assertEquals("CUSTOM_istatCode", response.getIstatCode());
        Assertions.assertEquals("CUSTOM_legalForm", response.getLegalForm());
    }

    @Test
    void createInstitutionStrategyRawWithDefaultOriginAndOriginId() {
        institution.setIstatCode("CUSTOM_istatCode");
        institution.setLegalForm("CUSTOM_legalForm");
        when(institutionConnector.findByTaxCodeAndSubunitCode("taxCode", null, null)).thenReturn(Collections.emptyList());
        when(institutionConnector.save(institution)).thenReturn(institution);
        Institution response = strategy.createInstitution(CreateInstitutionStrategyInput.builder().taxCode("taxCode").build());
        Assertions.assertEquals("SELC", response.getOrigin());
        Assertions.assertEquals("SELC_taxCode", response.getOriginId());
        Assertions.assertEquals("CUSTOM_istatCode", response.getIstatCode());
        Assertions.assertEquals("CUSTOM_legalForm", response.getLegalForm());
    }
}
