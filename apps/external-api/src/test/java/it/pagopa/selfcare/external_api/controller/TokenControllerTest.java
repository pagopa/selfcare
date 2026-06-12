package it.pagopa.selfcare.external_api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.external_api.service.TokenService;
import it.pagopa.selfcare.external_api.model.token.TokenOnboardedUsers;
import it.pagopa.selfcare.external_api.mapper.TokenResourceMapperImpl;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest extends BaseControllerTest {

    private static final String BASE_URL = "/v1/tokens";

    @InjectMocks
    protected TokenController tokenController;
    @Mock
    private TokenService tokenService;

    @Spy
    private TokenResourceMapperImpl tokenResourceMapperImpl;

    @BeforeEach
    void setUp(){
        super.setUp(tokenController);
    }

    @Test
    void getTokensByProductFound() throws Exception {
        String productId = "productId";

        ClassPathResource inputResource = new ClassPathResource("expectations/TokenOnboardedUsers.json");
        List<TokenOnboardedUsers> tokens = objectMapper.readValue(Files.readAllBytes(inputResource.getFile().toPath()), new TypeReference<>() {});

        ClassPathResource outputResource = new ClassPathResource("expectations/TokensResource.json");
        String expectedResource = StringUtils.deleteWhitespace(new String(Files.readAllBytes(outputResource.getFile().toPath())));

        when(tokenService.findByProductId(productId, 1, 10, OnboardingStatus.REJECTED.name()))
                .thenReturn(tokens);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/products/{productId}", productId)
                        .queryParam("page", "1")
                        .queryParam("size", "10")
                        .queryParam("status", OnboardingStatus.REJECTED.name())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResource))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn();
    }

    @Test
    void getTokensByProductNotFound() throws Exception {
        String productId = "productId";

        when(tokenService.findByProductId(productId, 1, 10, null))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/products/{productId}", productId)
                        .queryParam("page", "1")
                        .queryParam("size", "10")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andReturn();
    }
}
