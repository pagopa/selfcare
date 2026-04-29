package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.core.strategy.CreateInstitutionStrategy;
import it.pagopa.selfcare.mscore.core.strategy.factory.CreateInstitutionStrategyFactory;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InstitutionServiceImplPdaTest {
    @InjectMocks
    private InstitutionServiceImpl institutionServiceImpl;
    @Mock
    private CreateInstitutionStrategyFactory createInstitutionStrategyFactory;

    @Mock
    private CreateInstitutionStrategy createInstitutionStrategy;

    @Test
    void testCreateInstitutionFromPda() {
        when(createInstitutionStrategyFactory.createInstitutionStrategyPda(any())).thenReturn(createInstitutionStrategy);
        when(createInstitutionStrategy.createInstitution(any())).thenReturn(new Institution());
        Institution institution = institutionServiceImpl.createInstitutionFromPda(new Institution(), "EC");
        assertNotNull(institution);
    }
}
