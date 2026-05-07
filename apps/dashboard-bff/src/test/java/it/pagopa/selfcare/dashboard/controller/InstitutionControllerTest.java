package it.pagopa.selfcare.dashboard.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.dashboard.model.GeographicTaxonomyDto;
import it.pagopa.selfcare.dashboard.model.GeographicTaxonomyListDto;
import it.pagopa.selfcare.dashboard.model.InstitutionResource;
import it.pagopa.selfcare.dashboard.model.delegation.*;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.institution.UpdateInstitutionResource;
import it.pagopa.selfcare.dashboard.model.mapper.DelegationMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionResourceMapperImpl;
import it.pagopa.selfcare.dashboard.model.product.ProductTree;
import it.pagopa.selfcare.dashboard.service.DelegationService;
import it.pagopa.selfcare.dashboard.service.FileStorageService;
import it.pagopa.selfcare.dashboard.service.InstitutionService;
import it.pagopa.selfcare.product.entity.Product;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InstitutionControllerTest extends BaseControllerTest {

    private static final String BASE_URL = "/v1/institutions";
    private static final String FILE_JSON_PATH = "src/test/resources/json/";
    private static final ProductTree PRODUCT_TREE;
    private static final Product PRODUCT;

    static {
        PRODUCT_TREE = new ProductTree();
        PRODUCT = new Product();
        PRODUCT.setId("id1");
        PRODUCT_TREE.setChildren(List.of(PRODUCT));
        PRODUCT_TREE.setNode(PRODUCT);
    }

    @InjectMocks
    private InstitutionController institutionController;
    @Mock
    private FileStorageService storageServiceMock;
    @Mock
    private InstitutionService institutionServiceMock;
    @Mock
    private DelegationService delegationService;
    @Spy
    private InstitutionResourceMapperImpl institutionResourceMapper;
    @Spy
    private DelegationMapperImpl delegationMapper;

    @BeforeEach
    public void setUp() {
        super.setUp(institutionController);
    }

    @Test
    void saveInstitutionLogo() throws Exception {
        // given
        String institutionId = "institutionId";
        String contentType = MimeTypeUtils.IMAGE_JPEG_VALUE;
        String filename = "test.jpeg";
        byte[] content = "test institution logo".getBytes();
        MockMultipartFile multipartFile = new MockMultipartFile("logo", filename, contentType, content);
        MockMultipartHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart(BASE_URL + "/" + institutionId + "/logo")
                .file(multipartFile);
        requestBuilder.with(request -> {
            request.setMethod(HttpMethod.PUT.name());
            return request;
        });

        // when
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk());
        // then
        ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(storageServiceMock, times(1))
                .storeInstitutionLogo(eq(institutionId), inputStreamCaptor.capture(), eq(contentType), eq(filename));
        InputStream capturedInputStream = inputStreamCaptor.getValue();
        byte[] capturedContent = capturedInputStream.readAllBytes();
        assertArrayEquals(content, capturedContent);
        verifyNoMoreInteractions(storageServiceMock);
    }

    @Test
    void getProductsTree_notNull() throws Exception {
        // given
        byte[] productTreeStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "ProductTree.json"));
        ProductTree productTree = objectMapper.readValue(productTreeStream, ProductTree.class);

        when(institutionServiceMock.getProductsTree())
                .thenReturn(singletonList(productTree));

        mockMvc.perform(get(BASE_URL + "/products")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(new String(Files.readAllBytes(Paths.get(FILE_JSON_PATH + "ProductsResource.json")))))
                .andReturn();
    }

    @Test
    void getProductsTree_empty() throws Exception {
        when(institutionServiceMock.getProductsTree())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get(BASE_URL + "/products")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andReturn();

        verify(institutionServiceMock, times(1))
                .getProductsTree();
        verifyNoMoreInteractions(institutionServiceMock);
    }

    @Test
    void updateInstitutionDescription_ok() throws Exception {
        //given
        String institutionId = "setId";
        UpdateInstitutionResource resource = mockInstance(new UpdateInstitutionResource());
        Institution institutionMock = mockInstance(new Institution());
        when(institutionServiceMock.updateInstitutionDescription(institutionId, resource)).thenReturn(institutionMock);

        //when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/" + institutionId)
                        .content(objectMapper.writeValueAsString(resource))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        //then
        Institution institutionResult = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertNotNull(institutionResult);
        assertEquals(institutionResult.getId(), institutionId);
        assertEquals(institutionResult.getDescription(), resource.getDescription());
        assertEquals(institutionResult.getDigitalAddress(), resource.getDigitalAddress());
        verify(institutionServiceMock, times(1))
                .updateInstitutionDescription(institutionId, resource);
        verifyNoMoreInteractions(institutionServiceMock);
    }

    /**
     * Method under test: {@link InstitutionController#getDelegationsUsingFrom(String, String)}
     */
    @Test
    void getDelegationsUsingFrom_shouldGetData() throws Exception {
        // Given
        Delegation expectedDelegation = dummyDelegation();
        GetDelegationParameters getDelegationParameters = dummyDelegationParametersFrom();

        when(delegationService.getDelegations(getDelegationParameters)).thenReturn(List.of(expectedDelegation));
        // When

        MvcResult result = mockMvc
                .perform(get(BASE_URL + "/{institutionId}/partners?productId={productId}", expectedDelegation.getInstitutionId(), expectedDelegation.getProductId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andReturn();

        List<DelegationResource> resource = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });
        // Then
        assertThat(resource).isNotNull();
        org.assertj.core.api.Assertions.assertThat(resource).hasSize(1);
        DelegationResource actual = resource.get(0);
        assertThat(actual.getId()).isEqualTo(expectedDelegation.getId());
        assertThat(actual.getInstitutionName()).isEqualTo(expectedDelegation.getInstitutionName());
        assertThat(actual.getBrokerName()).isEqualTo(expectedDelegation.getBrokerName());
        assertThat(actual.getBrokerId()).isEqualTo(expectedDelegation.getBrokerId());
        assertThat(actual.getProductId()).isEqualTo(expectedDelegation.getProductId());
        assertThat(actual.getInstitutionId()).isEqualTo(expectedDelegation.getInstitutionId());

        verify(delegationService, times(1))
                .getDelegations(getDelegationParameters);
        verifyNoMoreInteractions(delegationService);
    }

    /**
     * Method under test: {@link InstitutionController#getDelegationsUsingTo(String, String, String, Order, Integer, Integer)}
     */

    @Test
    void getDelegationsUsingTo_shouldGetData() throws Exception {
        // Given
        Delegation expectedDelegation = dummyDelegation();
        GetDelegationParameters delegationParameters = dummyDelegationParametersTo();

        when(delegationService.getDelegations(delegationParameters)).thenReturn(List.of(expectedDelegation));
        // When

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/institutions?productId={productId}&search={search}&taxCode={taxCode}&mode=FULL&order=ASC&page={page}&size={size}",
                                delegationParameters.getTo(), delegationParameters.getProductId(), delegationParameters.getSearch(),
                                delegationParameters.getTaxCode(), delegationParameters.getPage(), delegationParameters.getSize()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andReturn();

        List<DelegationResource> resource = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });
        // Then
        assertThat(resource).isNotNull();
        org.assertj.core.api.Assertions.assertThat(resource).hasSize(1);
        DelegationResource actual = resource.get(0);
        assertThat(actual.getId()).isEqualTo(expectedDelegation.getId());
        assertThat(actual.getInstitutionName()).isEqualTo(expectedDelegation.getInstitutionName());
        assertThat(actual.getBrokerName()).isEqualTo(expectedDelegation.getBrokerName());
        assertThat(actual.getBrokerId()).isEqualTo(expectedDelegation.getBrokerId());
        assertThat(actual.getProductId()).isEqualTo(expectedDelegation.getProductId());
        assertThat(actual.getInstitutionId()).isEqualTo(expectedDelegation.getInstitutionId());

        verify(delegationService, times(1))
                .getDelegations(dummyDelegationParametersTo());
        verifyNoMoreInteractions(delegationService);
    }

    @Test
    void getDelegationsUsingTo_shouldGetDataWithoutFilters() throws Exception {
        // Given
        Delegation expectedDelegation = dummyDelegation();

        GetDelegationParameters delegationParameters = GetDelegationParameters.builder()
                .to("to")
                .build();

        when(delegationService.getDelegations(delegationParameters)).thenReturn(List.of(expectedDelegation));
        // When

        MvcResult result = mockMvc
                .perform(get(BASE_URL + "/{institutionId}/institutions", expectedDelegation.getBrokerId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andReturn();

        List<DelegationResource> resource = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });
        // Then
        assertThat(resource).isNotNull();
        org.assertj.core.api.Assertions.assertThat(resource).hasSize(1);
        DelegationResource actual = resource.get(0);
        assertThat(actual.getId()).isEqualTo(expectedDelegation.getId());
        assertThat(actual.getInstitutionName()).isEqualTo(expectedDelegation.getInstitutionName());
        assertThat(actual.getInstitutionRootName()).isEqualTo(expectedDelegation.getInstitutionRootName());
        assertThat(actual.getBrokerName()).isEqualTo(expectedDelegation.getBrokerName());
        assertThat(actual.getBrokerId()).isEqualTo(expectedDelegation.getBrokerId());
        assertThat(actual.getProductId()).isEqualTo(expectedDelegation.getProductId());
        assertThat(actual.getInstitutionId()).isEqualTo(expectedDelegation.getInstitutionId());

        verify(delegationService, times(1))
                .getDelegations(delegationParameters);
        verifyNoMoreInteractions(delegationService);
    }

    @Test
    void testGetInstitution() throws Exception {
        String institutionId = "123";
        Institution institution = new Institution();
        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setId(institutionId);

        when(institutionServiceMock.findInstitutionById(institutionId)).thenReturn(institution);

        ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/{institutionId}", institutionId)
                .accept(MediaType.APPLICATION_JSON_VALUE));

        MvcResult mvcResult = resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        InstitutionResource resource = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(), new TypeReference<>() {
                });

        Assertions.assertEquals(resource.getId(), institution.getId());

    }

    @Test
    void testUpdateInstitutionGeographicTaxonomy() throws Exception {
        String institutionId = "123";
        GeographicTaxonomyListDto geographicTaxonomyListDto = new GeographicTaxonomyListDto();
        GeographicTaxonomyDto geo = new GeographicTaxonomyDto();
        geo.setCode("123");
        geo.setDesc("desc");
        geographicTaxonomyListDto.setGeographicTaxonomyDtoList(List.of(geo));

        mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/" + institutionId + "/geographic-taxonomy")
                        .content(objectMapper.writeValueAsString(geographicTaxonomyListDto))
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }


    private Delegation dummyDelegation() {
        Delegation delegation = new Delegation();
        delegation.setInstitutionId("from");
        delegation.setBrokerId("to");
        delegation.setId("setId");
        delegation.setProductId("setProductId");
        delegation.setType(DelegationType.PT);
        delegation.setInstitutionName("setInstitutionFromName");
        return delegation;
    }

    private GetDelegationParameters dummyDelegationParametersTo() {
        return GetDelegationParameters.builder()
                .to("to")
                .productId("setProductId")
                .search("name")
                .order(Order.ASC.name())
                .page(0)
                .size(1000)
                .build();
    }

    private GetDelegationParameters dummyDelegationParametersFrom() {
        return GetDelegationParameters.builder()
                .from("from")
                .productId("setProductId")
                .build();
    }

}