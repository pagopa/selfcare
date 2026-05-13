package it.pagopa.selfcare.external_api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.external_api.mapper.InstitutionResourceMapperImpl;
import it.pagopa.selfcare.external_api.mapper.ProductsMapper;
import it.pagopa.selfcare.external_api.mapper.ProductsMapperImpl;
import it.pagopa.selfcare.external_api.mapper.UserInfoResourceMapperImpl;
import it.pagopa.selfcare.external_api.model.document.ResourceResponse;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardedInstitutionResource;
import it.pagopa.selfcare.external_api.model.product.ProductResource;
import it.pagopa.selfcare.external_api.model.user.UserProductResponse;
import it.pagopa.selfcare.external_api.service.ContractService;
import it.pagopa.selfcare.external_api.service.InstitutionService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InstitutionControllerV2Test extends BaseControllerTest {

    private static final String BASE_URL = "/v2/institutions";

    @InjectMocks

    private InstitutionV2Controller institutionV2Controller;
    @Mock
    private InstitutionService institutionService;

    @Mock
    private UserService userService;

    @Mock
    private ContractService contractService;
    @Spy
    private ProductsMapper productsMapper = new ProductsMapperImpl();
    @Spy
    private InstitutionResourceMapperImpl institutionResourceMapperImpl;

    @Spy
    private UserInfoResourceMapperImpl userInfoResourceMapper;

    @BeforeEach
    void setUp(){
        super.setUp(institutionV2Controller);
    }

    @Test
    void getInstitutionsWith2Elements() throws Exception {

        ClassPathResource inputResource = new ClassPathResource("expectations/OnboardedInstitutionInfoList.json");
        byte[] institutionInfoStream = Files.readAllBytes(inputResource.getFile().toPath());
        List<OnboardedInstitutionResource> onboardedInstitutionInfos = objectMapper.readValue(institutionInfoStream, new TypeReference<>() {});
        onboardedInstitutionInfos.forEach(onboardedInstitutionInfo -> onboardedInstitutionInfo.setStatus("ACTIVE"));

        ClassPathResource outputResource = new ClassPathResource("expectations/InstitutionResourceV2_2elements.json");
        String expectedResource = StringUtils.deleteWhitespace(new String(Files.readAllBytes(outputResource.getFile().toPath())));

        SelfCareUser selfCareUser = SelfCareUser.builder("id").name("nome").surname("cognome").build();
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(selfCareUser);
        final String productId = "productId";

        when(userService.getOnboardedInstitutionsDetailsActive(anyString(), anyString()))
                .thenReturn(onboardedInstitutionInfos);
        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .param("productId", productId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(content().string(expectedResource))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(status().isOk())
                .andReturn();
        //then

        verify(userService, times(1))
                .getOnboardedInstitutionsDetailsActive(selfCareUser.getId(), productId);
        verifyNoMoreInteractions(institutionService);
    }

    @Test
    void getInstitutionsWith1Elements() throws Exception {

        ClassPathResource inputResource = new ClassPathResource("expectations/OnboardedInstitutionInfoList.json");
        byte[] institutionInfoStream = Files.readAllBytes(inputResource.getFile().toPath());
        List<OnboardedInstitutionResource> onboardedInstitutionInfos = objectMapper.readValue(institutionInfoStream, new TypeReference<>() {});
        onboardedInstitutionInfos.remove(1);

        ClassPathResource outputResource = new ClassPathResource("expectations/InstitutionResourceV2_1element.json");
        String expectedResource = StringUtils.deleteWhitespace(new String(Files.readAllBytes(outputResource.getFile().toPath())));

        SelfCareUser selfCareUser = SelfCareUser.builder("id").name("nome").surname("cognome").build();
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(selfCareUser);
        final String productId = "productId";

        when(userService.getOnboardedInstitutionsDetailsActive(anyString(), anyString()))
                .thenReturn(onboardedInstitutionInfos);
        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .param("productId", productId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(content().string(expectedResource))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(status().isOk())
                .andReturn();
        //then

        verify(userService, times(1))
                .getOnboardedInstitutionsDetailsActive(selfCareUser.getId(), productId);
        verifyNoMoreInteractions(institutionService);
    }

    @Test
    void getInstitutionsWithoutElement() throws Exception {

        SelfCareUser selfCareUser = SelfCareUser.builder("id").name("nome").surname("cognome").build();
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(selfCareUser);
        final String productId = "productId";

        when(userService.getOnboardedInstitutionsDetailsActive(anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .param("productId", productId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(status().isOk())
                .andReturn();
        //then

        verify(userService, times(1))
                .getOnboardedInstitutionsDetailsActive(selfCareUser.getId(), productId);
        verifyNoMoreInteractions(institutionService);
    }

    @Test
    void getInstitutionsWithoutProductId() throws Exception {

        Authentication authentication = mock(Authentication.class);
        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void getContractOk() throws Exception {

        String institutionId = "institutionId";
        String productId = "productId";

        ResourceResponse resourceResponse = new ResourceResponse();
        resourceResponse.setData("data".getBytes());
        resourceResponse.setFileName("fileName");
        resourceResponse.setMimetype("mimetype");
        when(contractService.getContractV2(institutionId, productId)).thenReturn(resourceResponse);

        MockMvcBuilders.standaloneSetup(institutionV2Controller)
                .build().perform(MockMvcRequestBuilders.get(BASE_URL + "/{institutionId}/contract", institutionId)
                .param("productId", productId).accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s", resourceResponse.getFileName())))
                .andExpect(content().bytes(resourceResponse.getData()));
    }

    @Test
    void getInstitutionUserProductsWith2Elements() throws Exception {
        ClassPathResource inputResource = new ClassPathResource("expectations/Product.json");
        byte[] productStream = Files.readAllBytes(inputResource.getFile().toPath());
        List<ProductResource> products = objectMapper.readValue(productStream, new TypeReference<>() {});

        products.forEach(
            productResource -> {
              productResource.setContractTemplatePath("contractTemplatePath");
              productResource.setContractTemplateVersion("1.0.0");
            });

        ClassPathResource outputResource = new ClassPathResource("expectations/ProductResources.json");
        String expectedResource = StringUtils.deleteWhitespace(new String(Files.readAllBytes(outputResource.getFile().toPath())));

        when(institutionService.getInstitutionUserProductsV2(anyString(), anyString())).thenReturn(products);

        mockMvc.perform(get("/v2/institutions/{institutionId}/products", "testInstitutionId")
                        .param("userId", "testUserId")
                .contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE))
                .andExpect(content().string(expectedResource))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andReturn();
    }

    @Test
    void getInstitutionUserProductsWithEmptyList() throws Exception {


        when(institutionService.getInstitutionUserProductsV2("testInstitutionId", "testUserId")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v2/institutions/{institutionId}/products", "testInstitutionId")
                        .param("userId", "testUserId")
                .contentType(APPLICATION_JSON_VALUE).accept(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andReturn();
    }


    @Test
    void getInstitutionUsersByProductsWith2ReturnedElements() throws Exception {

        ClassPathResource productResponse = new ClassPathResource("expectations/UserInfo.json");
        byte[] userInfoStream = Files.readAllBytes(productResponse.getFile().toPath());
        Collection<UserProductResponse> userInfo = objectMapper.readValue(userInfoStream, new TypeReference<>() {});

        ClassPathResource outputResource = new ClassPathResource("expectations/UserResource.json");
        String expectedResource = StringUtils.deleteWhitespace(new String(Files.readAllBytes(outputResource.getFile().toPath())));

        when(institutionService.getInstitutionProductUsersV2("testInstitutionId", "testProductId", null, null, null)).thenReturn(userInfo);

        mockMvc.perform(get("/v2/institutions/{institutionId}/users", "testInstitutionId")
                        .param("productId", "testProductId")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(content().string(expectedResource))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andReturn();
    }

    @Test
    void getInstitutionUsersByProductsWithEmptyList() throws Exception {

        when(institutionService.getInstitutionProductUsersV2("testInstitutionId", "testProductId", null, null, null))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v2/institutions/{institutionId}/users", "testInstitutionId")
                        .param("productId", "testProductId")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andReturn();
    }
}
