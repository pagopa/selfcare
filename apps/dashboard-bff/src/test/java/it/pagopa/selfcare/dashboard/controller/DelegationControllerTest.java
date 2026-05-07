package it.pagopa.selfcare.dashboard.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationId;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationIdResource;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationRequestDto;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationType;
import it.pagopa.selfcare.dashboard.model.mapper.DelegationMapperImpl;
import it.pagopa.selfcare.dashboard.service.DelegationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.file.Files;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DelegationControllerTest extends BaseControllerTest {

    @InjectMocks
    private DelegationController delegationController;

    @Mock
    private DelegationService delegationService;
    @Spy
    private DelegationMapperImpl delegationMapper;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        super.setUp(delegationController);
        objectMapper = new ObjectMapper();
    }

    @Test
    void createDelegationWithRequiredParameter() throws Exception {
        DelegationId delegationId = new DelegationId();
        delegationId.setId("id");

        DelegationIdResource delegationIdResource = new DelegationIdResource();
        delegationIdResource.setId("id");

        DelegationRequestDto delegationRequest = getDelegationRequestDto();

        when(delegationService.createDelegation((delegationMapper.toDelegation(delegationRequest)))).thenReturn(delegationId);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/delegations")
                        .content(objectMapper.writeValueAsString(getDelegationRequestDto()))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(content().string(objectMapper.writeValueAsString(delegationIdResource)))
                .andReturn();
    }

    @Test
    void createDelegationWithFromNull() throws Exception {
        DelegationRequestDto delegationRequestDto = getDelegationRequestDto();
        delegationRequestDto.setFrom(null);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/delegations")
                        .content(objectMapper.writeValueAsString(delegationRequestDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void createDelegationWithToNull() throws Exception {
        DelegationRequestDto delegationRequestDto = getDelegationRequestDto();
        delegationRequestDto.setTo(null);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/delegations")
                        .content(objectMapper.writeValueAsString(delegationRequestDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void createDelegationWithProductIdNull() throws Exception {
        DelegationRequestDto delegationRequestDto = getDelegationRequestDto();
        delegationRequestDto.setProductId(null);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/delegations")
                        .content(objectMapper.writeValueAsString(delegationRequestDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void createDelegationWithInstitutionFromNameNull() throws Exception {
        DelegationRequestDto delegationRequestDto = getDelegationRequestDto();
        delegationRequestDto.setInstitutionFromName(null);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/delegations")
                        .content(objectMapper.writeValueAsString(delegationRequestDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void createDelegationWithInstitutionToNameNull() throws Exception {
        DelegationRequestDto delegationRequestDto = getDelegationRequestDto();
        delegationRequestDto.setInstitutionToName(null);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/delegations")
                        .content(objectMapper.writeValueAsString(delegationRequestDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void createDelegationWithWrongType() throws Exception {
        DelegationRequestDto delegationRequestDto = getDelegationRequestDto();
        delegationRequestDto.setType(DelegationType.UO);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/delegations")
                        .content(objectMapper.writeValueAsString(delegationRequestDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    private DelegationRequestDto getDelegationRequestDto() throws IOException {
        ClassPathResource resource = new ClassPathResource("json/delegationRequestDto.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        return objectMapper.readValue(resourceStream, new TypeReference<>() {
        });
    }
}