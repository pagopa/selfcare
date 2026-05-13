package it.pagopa.selfcare.external_api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionResponse;
import it.pagopa.selfcare.external_api.client.*;
import it.pagopa.selfcare.external_api.mapper.ProductsMapperImpl;
import it.pagopa.selfcare.external_api.mapper.RegistryProxyMapperImpl;
import it.pagopa.selfcare.external_api.mapper.UserMapperImpl;
import it.pagopa.selfcare.external_api.model.institution.GeographicTaxonomy;
import it.pagopa.selfcare.external_api.model.institution.Institution;
import it.pagopa.selfcare.external_api.model.institution.Institutions;
import it.pagopa.selfcare.external_api.model.institution.SearchMode;
import it.pagopa.selfcare.external_api.model.national_registries.LegalVerification;
import it.pagopa.selfcare.external_api.model.pnpg.CreatePnPgInstitution;
import it.pagopa.selfcare.external_api.model.product.PartyProduct;
import it.pagopa.selfcare.external_api.model.product.ProductOnboardingStatus;
import it.pagopa.selfcare.external_api.model.product.ProductResource;
import it.pagopa.selfcare.external_api.model.user.User;
import it.pagopa.selfcare.external_api.model.user.UserProductResponse;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.registry_proxy.generated.openapi.v1.dto.LegalVerificationResult;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserDataResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.ValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static it.pagopa.selfcare.external_api.model.user.RelationshipState.ACTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {InstitutionServiceImpl.class, UserMapperImpl.class,
        RegistryProxyMapperImpl.class, ProductsMapperImpl.class})
@TestPropertySource(locations = "classpath:config/core-config.properties")
class InstitutionServiceImplTest extends BaseServiceTestUtils {

    @Autowired
    private InstitutionServiceImpl institutionService;

    @MockBean
    private MsCoreRestClient msCoreRestClient;

    @MockBean
    private it.pagopa.selfcare.product.service.ProductService productService;

    @MockBean
    private MsUserApiRestClient msUserApiRestClient;

    @MockBean
    private MsRegistryProxyNationalRegistryRestClient nationalRegistryRestClient;

    @MockBean
    private UserRegistryRestClient userRegistryRestClient;

    @MockBean
    private MsCoreInstitutionApiClient institutionApiClient;

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void getInstitutionUserProducts_WithProductEmpty() {
        String institutionId = "institutionId";
        PartyProduct partyProduct = new PartyProduct();
        partyProduct.setStatus(ProductOnboardingStatus.ACTIVE);
        partyProduct.setRole(PartyRole.MANAGER);
        partyProduct.setId("123");
        String userId = UUID.randomUUID().toString();
        Assertions.assertEquals(0, institutionService.getInstitutionUserProductsV2(institutionId, userId).size());
    }

    @Test
    void getInstitutionUserProducts_WithOneMatch() {
        String institutionId = "institutionId";

        String userId = UUID.randomUUID().toString();

        UserDataResponse userDataResponse = new UserDataResponse();
        userDataResponse.setUserId(userId);
        it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse onboardedProductResponse = new it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse();
        onboardedProductResponse.setProductId("id");
        it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse onboardedProductResponse2 = new it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse();
        onboardedProductResponse2.setProductId("123");
        userDataResponse.setProducts(List.of(onboardedProductResponse, onboardedProductResponse2));
        when(msUserApiRestClient._retrieveUsers(institutionId, userId, userId, null, null, null, List.of(ACTIVE.name())))
                .thenReturn(ResponseEntity.ok(List.of(userDataResponse)));

        Product product = dummyProduct("456");
        Product product2 = dummyProduct("123");
        when(productService.getProducts(true, true)).thenReturn(List.of(product, product2));
        when(institutionApiClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(dummyInstitutionResponse()) );

        List<ProductResource> productResources = institutionService.getInstitutionUserProductsV2(institutionId, userId);
        Assertions.assertEquals(1, productResources.size());
        assertEquals("setContractTemplatePath", productResources.get(0).getContractTemplatePath());
        assertEquals("setContractTemplateVersion", productResources.get(0).getContractTemplateVersion());
    }

    InstitutionResponse dummyInstitutionResponse() {
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setInstitutionType(InstitutionType.PA.name());
        return institutionResponse;
    }

    Product dummyProduct(String id) {
        Product product = new Product();
        product.setId(id);

        ContractTemplate contractTemplate = new ContractTemplate();
        contractTemplate.setContractTemplatePath("setContractTemplatePath");
        contractTemplate.setContractTemplateVersion("setContractTemplateVersion");
        product.setInstitutionContractMappings(Map.of(InstitutionType.PA.name(), contractTemplate));
        return product;
    }

    @Test
    void getInstitutionUserProducts_WithTwoMatch() {
        String institutionId = "institutionId";

        String userId = UUID.randomUUID().toString();

        UserDataResponse userDataResponse = new UserDataResponse();
        userDataResponse.setUserId(userId);
        it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse onboardedProductResponse = new it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse();
        onboardedProductResponse.setProductId("id");
        it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse onboardedProductResponse2 = new it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse();
        onboardedProductResponse.setProductId("id2");
        userDataResponse.setProducts(List.of(onboardedProductResponse, onboardedProductResponse2));
        when(msUserApiRestClient._retrieveUsers(institutionId, userId, userId, null, null, null, List.of(ACTIVE.name())))
                .thenReturn(ResponseEntity.ok(List.of(userDataResponse)));

        Product product = new Product();
        product.setId("id");
        Product product2 = new Product();
        product.setId("id2");
        when(productService.getProducts(true, true)).thenReturn(List.of(product, product2));
        when(institutionApiClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(new InstitutionResponse()) );

        List<ProductResource> expectation = institutionService.getInstitutionUserProductsV2(institutionId, userId);
        Assertions.assertEquals(2, expectation.size());
    }

    @Test
    void getInstitutionProductUserWithoutInstitutionId() {
        String productId = "productId";
        String userId = "userId";
        Executable executable = () -> institutionService.getInstitutionProductUsersV2(null, productId, userId, null, null);
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("An Institution id is required", e.getMessage());
    }

    @Test
    void getInstitutionProductUserWithoutProductId() {
        String institutionId = "institutionId";
        String userId = "userId";
        Executable executable = () -> institutionService.getInstitutionProductUsersV2(institutionId, null, userId, null, null);
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("A Product id is required", e.getMessage());
    }

    @Test
    void getInstitutionProductUserV2EmptyList() throws Exception {
        String institutionId = "institutionId";
        String productId = "productId";
        String userId = UUID.randomUUID().toString();
        String xSelfCareUid = "onboarding-interceptor";
        ClassPathResource resource = new ClassPathResource("expectations/UserInstitution.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        List<UserInstitutionResponse> userInstitutions = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });
        userInstitutions.forEach(userInstitutionResponse -> userInstitutionResponse.setProducts(Collections.emptyList()));
        Mockito.when(msUserApiRestClient._retrievePaginatedAndFilteredUser(institutionId, null, null, List.of(productId), null, null, List.of(ACTIVE.name()), userId))
                .thenReturn(ResponseEntity.ok(userInstitutions));


        ClassPathResource userResource = new ClassPathResource("expectations/User.json");
        byte[] userStream = Files.readAllBytes(userResource.getFile().toPath());
        User user = objectMapper.readValue(userStream, User.class);
        when(userRegistryRestClient.getUserByInternalId(any(), any())).thenReturn(user);
        Collection<UserProductResponse> expectation = institutionService.getInstitutionProductUsersV2(institutionId, productId, userId, null, xSelfCareUid);
        Assertions.assertEquals(0, expectation.size());
    }

    @Test
    void getInstitutionProductUserV2WithUuidInServiceType() throws Exception {
        String institutionId = "id";
        String productId = "productId";
        String userId = "123e4567-e89b-12d3-a456-426614174000";
        String xSelfCareUid = "onboarding-interceptor";

        ClassPathResource resource = new ClassPathResource("expectations/UserInstitutionV2.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        List<UserInstitutionResponse> userInstitutions = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });
        Mockito.when(msUserApiRestClient._retrievePaginatedAndFilteredUser(institutionId, null, null, List.of(productId), null, null, List.of(ACTIVE.name()), userId))
                .thenReturn(ResponseEntity.ok(userInstitutions));
        when(productService.getProductRaw(productId)).thenReturn(dummyProduct(productId));

        ClassPathResource userResource = new ClassPathResource("expectations/User.json");
        byte[] userStream = Files.readAllBytes(userResource.getFile().toPath());
        User user = objectMapper.readValue(userStream, User.class);
        when(userRegistryRestClient.getUserByInternalId(any(), any())).thenReturn(user);
        Collection<UserProductResponse> result = institutionService.getInstitutionProductUsersV2(institutionId, productId, userId, null, xSelfCareUid);

        ClassPathResource userInfoResource = new ClassPathResource("expectations/UserInfoV2.json");
        byte[] userInfoStrean = Files.readAllBytes(userInfoResource.getFile().toPath());
        List<UserProductResponse> expectation = objectMapper.readValue(userInfoStrean, new TypeReference<>() {
        });

        Assertions.assertEquals(1, expectation.size());
        Assertions.assertEquals(objectMapper.writeValueAsString(expectation), objectMapper.writeValueAsString(result));
    }

    @Test
    void getInstitutionProductUserV2WithoutUuidInServiceType() throws Exception {
        String institutionId = "id";
        String productId = "productId";
        String userId = "123e4567-e89b-12d3-a456-426614174000";
        String xSelfCareUid = "uuid";

        ClassPathResource resource = new ClassPathResource("expectations/UserInstitutionV2.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        List<UserInstitutionResponse> userInstitutions = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });
        Mockito.when(msUserApiRestClient._retrievePaginatedAndFilteredUser(institutionId, null, null, List.of(productId), null, null, List.of(ACTIVE.name()), userId))
                .thenReturn(ResponseEntity.ok(userInstitutions));

        ClassPathResource userResource = new ClassPathResource("expectations/UserV2.json");
        byte[] userStream = Files.readAllBytes(userResource.getFile().toPath());
        User user = objectMapper.readValue(userStream, User.class);
        when(userRegistryRestClient.getUserByInternalId(any(), any())).thenReturn(user);
        when(productService.getProductRaw(productId)).thenReturn(dummyProduct(productId));

        Collection<UserProductResponse> result = institutionService.getInstitutionProductUsersV2(institutionId, productId, userId, null, xSelfCareUid);

        ClassPathResource userInfoResource = new ClassPathResource("expectations/UserInfoWithoutTaxCode.json");
        byte[] userInfoStream = Files.readAllBytes(userInfoResource.getFile().toPath());
        List<UserProductResponse> expectation = objectMapper.readValue(userInfoStream, new TypeReference<>() {
        });

        Assertions.assertEquals(1, expectation.size());
        Assertions.assertEquals(objectMapper.writeValueAsString(expectation), objectMapper.writeValueAsString(result));
    }

    @Test
    void getGeographicTaxonomyListWithoutInstitutionId() {
        Executable executable = () -> institutionService.getGeographicTaxonomyList(null);
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("An Institution id is required", e.getMessage());
    }

    @Test
    void getGeographicTaxonomyListWithEmptyList() {
        String institutionId = "institutionId";
        when(msCoreRestClient.getInstitution(institutionId)).thenReturn(new Institution());
        Assertions.assertThrows(ValidationException.class, () -> institutionService.getGeographicTaxonomyList(institutionId));
    }

    @Test
    void getGeographicTaxonomyList() {
        String institutionId = "institutionId";
        GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
        geographicTaxonomy.setCode("testCode1");
        geographicTaxonomy.setDesc("testDesc1");
        GeographicTaxonomy geographicTaxonomy2 = new GeographicTaxonomy();
        geographicTaxonomy2.setCode("testCode2");
        geographicTaxonomy2.setDesc("testDesc2");
        Institution institution = new Institution();
        institution.setGeographicTaxonomies(List.of(geographicTaxonomy, geographicTaxonomy2));
        when(msCoreRestClient.getInstitution(institutionId)).thenReturn(institution);

        List<GeographicTaxonomy> expectation = institutionService.getGeographicTaxonomyList(institutionId);
        Assertions.assertEquals(2, expectation.size());
        Assertions.assertEquals(geographicTaxonomy.getCode(), expectation.get(0).getCode());
        Assertions.assertEquals(geographicTaxonomy.getDesc(), expectation.get(0).getDesc());
        Assertions.assertEquals(geographicTaxonomy2.getCode(), expectation.get(1).getCode());
        Assertions.assertEquals(geographicTaxonomy2.getDesc(), expectation.get(1).getDesc());
    }

    @Test
    void getInstitutionsByGeoTaxonomiesWithoutGeoTaxonomies() {
        SearchMode searchMode = SearchMode.exact;
        Executable executable = () -> institutionService.getInstitutionsByGeoTaxonomies(Collections.emptySet(), searchMode);
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("GeoTaxonomy ids are required in order to retrieve the institutions", e.getMessage());
    }

    @Test
    void getInstitutionsByGeoTaxonomiesWithEmptyList() {
        Set<String> geoTaxIds = Set.of("geoTaxId1", "geoTaxId2");
        SearchMode searchMode = SearchMode.any;
        Institutions institutions = new Institutions();
        institutions.setItems(Collections.emptyList());
        when(msCoreRestClient.getInstitutionsByGeoTaxonomies(String.join(",", geoTaxIds),
                searchMode)).thenReturn(institutions);

        Collection<Institution> expectation = institutionService.getInstitutionsByGeoTaxonomies(geoTaxIds, searchMode);
        Assertions.assertEquals(0, expectation.size());
    }

    @Test
    void getInstitutionsByGeoTaxonomies() throws IOException {
        Set<String> geoTaxIds = Set.of("geoTaxId1", "geoTaxId2");
        SearchMode searchMode = SearchMode.any;
        ClassPathResource productResponse = new ClassPathResource("expectations/Institution.json");
        byte[] institutionStream = Files.readAllBytes(productResponse.getFile().toPath());
        List<Institution> institution = objectMapper.readValue(institutionStream, new TypeReference<>() {
        });
        Institutions institutions = new Institutions();
        institutions.setItems(institution);
        when(msCoreRestClient.getInstitutionsByGeoTaxonomies(String.join(",", geoTaxIds),
                searchMode)).thenReturn(institutions);
        Collection<Institution> expectation = institutionService.getInstitutionsByGeoTaxonomies(geoTaxIds, searchMode);
        Assertions.assertEquals(2, expectation.size());
        Assertions.assertEquals(expectation, institution);
    }

    @Test
    void addInstitution() {
        CreatePnPgInstitution createPnPgInstitution = new CreatePnPgInstitution();
        createPnPgInstitution.setDescription("description");
        createPnPgInstitution.setExternalId("taxId");
        String institutionId = UUID.randomUUID().toString();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId(institutionId);
        when(institutionApiClient._createPgInstitutionUsingPOST(any())).thenReturn(ResponseEntity.ok(institutionResponse));
        String expectation = institutionService.addInstitution(createPnPgInstitution);
        Assertions.assertEquals(institutionId, expectation);
    }

    @Test
    void verifyLegal() {
        final String taxId = "taxId";
        final String vatNumber = "vatNumber";

        Mockito.when(nationalRegistryRestClient._verifyLegalUsingGET(taxId, vatNumber))
                .thenReturn(ResponseEntity.ok(new LegalVerificationResult()));

        //when
        LegalVerification result = institutionService.verifyLegal(taxId, vatNumber);

        //then
        assertNotNull(result);

    }

}
