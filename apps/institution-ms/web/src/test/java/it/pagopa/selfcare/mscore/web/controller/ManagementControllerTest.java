package it.pagopa.selfcare.mscore.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.core.InstitutionService;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.mscore.web.model.institution.BulkPartiesSeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {ManagementController.class})
@ExtendWith(MockitoExtension.class)
class ManagementControllerTest {
    @Mock
    private InstitutionService institutionService;

    @InjectMocks
    private ManagementController managementController;

    @Test
    void testGetInstitutionsByIds() throws Exception {
        BulkPartiesSeed bulkPartiesSeed = new BulkPartiesSeed();
        bulkPartiesSeed.setPartyIdentifiers(List.of("42"));
        ObjectMapper mapper = new ObjectMapper();
        Institution institution = new Institution();
        institution.setId("id");
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId("productId");
        onboarding.setStatus(RelationshipState.ACTIVE);
        institution.setOnboarding(List.of(onboarding));
        when(institutionService.retrieveInstitutionByIds(any())).thenReturn(List.of(institution));
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/bulk/institutions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(bulkPartiesSeed));
        MockMvcBuilders.standaloneSetup(managementController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}

