package it.pagopa.selfcare.external_api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.external_api.mapper.NationalRegistryMapper;
import it.pagopa.selfcare.external_api.mapper.NationalRegistryMapperImpl;
import it.pagopa.selfcare.external_api.model.national_registries.LegalVerification;
import it.pagopa.selfcare.external_api.model.national_registries.VerifyRequestDto;
import it.pagopa.selfcare.external_api.service.InstitutionService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Files;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class NationalRegistriesControllerTest extends BaseControllerTest {
    private static final String BASE_URL = "/v2/national-registries";

    @InjectMocks
    private NationalRegistryController nationalRegistryController;
    @Mock
    private InstitutionService institutionService;
    @Spy
    private NationalRegistryMapper nationalRegistryMapper = new NationalRegistryMapperImpl();

    @BeforeEach
    void setUp(){super.setUp(nationalRegistryController);}


    @Test
    public void verifyLegal() throws Exception{
        ClassPathResource inputResource = new ClassPathResource("expectations/LegalVerify.json");
        byte[] legalVerifyStream = Files.readAllBytes(inputResource.getFile().toPath());
        LegalVerification legalVerification = objectMapper.readValue(legalVerifyStream, new TypeReference<>() {});
        ClassPathResource outputResource = new ClassPathResource("expectations/LegalVerify.json");
        String expectedResource = StringUtils.deleteWhitespace(new String(Files.readAllBytes(outputResource.getFile().toPath())));
        final String taxId = "taxId";
        final String vatNumber = "vatNumber";

        VerifyRequestDto verifyRequestDto = new VerifyRequestDto(taxId, vatNumber);
        when(institutionService.verifyLegal(anyString(), anyString())).thenReturn(legalVerification);

        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL+"/legal-tax/verification")
                        .content(objectMapper.writeValueAsString(verifyRequestDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(content().string(expectedResource))
                .andExpect(status().isOk())
                .andReturn();

        verify(institutionService, times(1)).verifyLegal(taxId, vatNumber);
    }





}
