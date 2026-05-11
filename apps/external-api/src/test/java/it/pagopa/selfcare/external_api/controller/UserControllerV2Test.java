package it.pagopa.selfcare.external_api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.external_api.mapper.UserInfoResourceMapperImpl;
import it.pagopa.selfcare.external_api.model.user.*;
import it.pagopa.selfcare.external_api.service.UserService;
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
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerV2Test extends BaseControllerTest {

    private static final String BASE_URL = "/v2/users";

    @InjectMocks
    protected UserV2Controller userV2Controller;

    @Mock
    private UserService userService;

    @Spy
    private UserInfoResourceMapperImpl userInfoResourceMapperImpl;

    @BeforeEach
    void setUp(){
        super.setUp(userV2Controller);
    }

    @Test
    void getUserInfoFound() throws Exception {

        ClassPathResource inputResource = new ClassPathResource("expectations/UserInfoWrapper.json");
        UserInfoWrapper userInfoWrapper = objectMapper.readValue(Files.readAllBytes(inputResource.getFile().toPath()), new TypeReference<>() {});

        ClassPathResource outputResource = new ClassPathResource("expectations/UserInfoResource.json");
        String userInfoResource = StringUtils.deleteWhitespace(new String(Files.readAllBytes(outputResource.getFile().toPath())));

        SearchUserDto searchUserDto = new SearchUserDto();
        searchUserDto.setFiscalCode("NLLGPJ67L30L783W");
        searchUserDto.setStatuses(List.of(RelationshipState.ACTIVE));

        when(userService.getUserInfoV2(searchUserDto.getFiscalCode(), searchUserDto.getStatuses(), "productId")).thenReturn(userInfoWrapper);

        mockMvc.perform(post(BASE_URL)
                        .content(objectMapper.writeValueAsString(searchUserDto))
                        .queryParam("productId", "productId")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(userInfoResource))
                .andExpect(jsonPath("$.onboardedInstitutions", hasSize(2)))
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andReturn();
    }

    @Test
    void getUserInfoNotFound() throws Exception {

        SearchUserDto searchUserDto = new SearchUserDto();
        searchUserDto.setFiscalCode("NLLGPJ67L30L783W");
        searchUserDto.setStatuses(List.of(RelationshipState.ACTIVE));

        when(userService.getUserInfoV2(searchUserDto.getFiscalCode(), searchUserDto.getStatuses(), null)).thenReturn(null);

        mockMvc.perform(post(BASE_URL)
                        .content(objectMapper.writeValueAsString(searchUserDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andReturn();
    }

    @Test
    void getUserInfoWithoutFiscalCode() throws Exception {

        SearchUserDto searchUserDto = new SearchUserDto();
        searchUserDto.setStatuses(List.of(RelationshipState.ACTIVE));

        mockMvc.perform(post(BASE_URL)
                        .content(objectMapper.writeValueAsString(searchUserDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void getUserProductInfoOk() throws Exception {

        String institutionId = "institutionId";
        String productId = "productId";
        String userId = "userId";
        UserDetailsWrapper userDetailsWrapper = new UserDetailsWrapper();
        userDetailsWrapper.setUserId(userId);
        userDetailsWrapper.setInstitutionId(institutionId);
        userDetailsWrapper.setProductDetails(buildProductDetails());

        ClassPathResource outputResource = new ClassPathResource("expectations/UserDetailsResource.json");
        String expectedResource = StringUtils.deleteWhitespace(new String(Files.readAllBytes(outputResource.getFile().toPath())));

        when(userService.getUserOnboardedProductsDetailsV2(anyString(), anyString(), anyString())).thenReturn(userDetailsWrapper);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{id}/onboarded-product", userId)
                        .queryParam("institutionId", institutionId)
                        .queryParam("productId", productId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResource))
                .andReturn();
    }

    @Test
    void getUserProductInfoNotFound() throws Exception {

        String institutionId = "institutionId";
        String productId = "productId";
        String userId = "userId";

        when(userService.getUserOnboardedProductsDetailsV2(userId, institutionId, productId)).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{id}/onboarded-product", userId)
                        .queryParam("institutionId", institutionId)
                        .queryParam("productId", productId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andReturn();
    }

    @Test
    void getUserProductInfoOkWithoutProductId() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{id}/onboarded-product", "id")
                        .queryParam("institutionId", "institutionId")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void getUserProductInfoOkWithoutInstitutionId() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{id}/onboarded-product", "id")
                        .queryParam("productId", "prod-io")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    private ProductDetails buildProductDetails() {
        ProductDetails product = new ProductDetails();
        product.setProductId("productId");
        product.setRoles(List.of("role"));
        product.setRole(PartyRole.MANAGER);
        product.setCreatedAt(OffsetDateTime.parse("2024-04-17T01:00:00+01:00"));
        return product;
    }



    @Test
    void getUserInstitution() throws Exception {

        final String userId = "userId";
        final String institutionId = "institutionId";
        final String productId = "productId";

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .queryParam("userId", userId)
                        .queryParam("institutionId", "institutionId")
                        .queryParam("products", productId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        verify(userService, times(1))
                .getUsersInstitutions(userId, institutionId, 0, 100, null, List.of(productId), null, null);
    }

}
