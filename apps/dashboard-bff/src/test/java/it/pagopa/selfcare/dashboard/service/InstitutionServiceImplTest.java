package it.pagopa.selfcare.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.commons.base.security.ProductGrantedAuthority;
import it.pagopa.selfcare.commons.base.security.SelfCareGrantedAuthority;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionPut;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionResponse;
import it.pagopa.selfcare.dashboard.client.CoreInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.model.institution.GeographicTaxonomy;
import it.pagopa.selfcare.dashboard.model.institution.GeographicTaxonomyList;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.institution.UpdateInstitutionResource;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.dashboard.model.product.ProductTree;
import it.pagopa.selfcare.dashboard.model.product.mapper.ProductMapper;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static it.pagopa.selfcare.commons.base.security.PartyRole.MANAGER;
import static it.pagopa.selfcare.commons.base.security.PartyRole.OPERATOR;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceImplTest extends BaseServiceTest {

    @InjectMocks
    private InstitutionServiceImpl institutionService;
    @Mock
    private CoreInstitutionApiRestClient coreInstitutionApiRestClient;
    @Mock
    private ProductService productService;
    @Spy
    private InstitutionMapperImpl institutionMapper;
    @Spy
    private ProductMapper productMapper;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getInstitutionById() throws IOException {

        String institutionId = "institutionId";

        ClassPathResource resource = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStream;
        try {
            resourceStream = Files.readAllBytes(resource.getFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
        InstitutionResponse institutionResponse = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionResponse));
        Institution institution = institutionMapper.toInstitution(institutionResponse);

        Institution result = institutionService.getInstitutionById(institutionId);

        Assertions.assertEquals(institution, result);
        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
    }

    @Test
    void getInstitutionByIdEmptyInstitution() {

        String institutionId = "institutionId";

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok().build());

        Institution result = institutionService.getInstitutionById(institutionId);
        Assertions.assertNull(result);
        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
    }

    @Test
    void updateInstitutionGeographicTaxonomy() {

        String institutionId = "institutionId";
        GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
        geographicTaxonomy.setCode("testCode1");
        geographicTaxonomy.setDesc("testDesc1");

        GeographicTaxonomyList geographicTaxonomyList = new GeographicTaxonomyList();
        geographicTaxonomyList.setGeographicTaxonomyList(List.of(geographicTaxonomy));

        InstitutionPut geographicTaxonomiesRequest = new InstitutionPut();
        geographicTaxonomiesRequest.setGeographicTaxonomyCodes(geographicTaxonomyList.getGeographicTaxonomyList().stream().map(GeographicTaxonomy::getCode).toList());

        institutionService.updateInstitutionGeographicTaxonomy(institutionId, geographicTaxonomyList);
        verify(coreInstitutionApiRestClient, times(1))._updateInstitutionUsingPUT(institutionId, geographicTaxonomiesRequest);
    }

    @Test
    void updateInstitutionGeographicTaxonomyWithoutInstitutionId() {

        GeographicTaxonomyList geographicTaxonomies = new GeographicTaxonomyList();
        Assertions.assertThrows(IllegalArgumentException.class, () -> institutionService.updateInstitutionGeographicTaxonomy(null, geographicTaxonomies));
    }

    @Test
    void updateInstitutionGeographicTaxonomyWithoutGeographicTaxonomies() {

        String institutionId = "institutionId";
        Assertions.assertThrows(IllegalArgumentException.class, () -> institutionService.updateInstitutionGeographicTaxonomy(institutionId, null));
    }

    @Test
    void getProductsTree() throws IOException {

        ClassPathResource resource = new ClassPathResource("json/ProductsResource.json");
        byte[] resourceStream;
        try {
            resourceStream = Files.readAllBytes(resource.getFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
        List<Product> product = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        when(productService.getProducts(false, true)).thenReturn(product);

        List<ProductTree> productTrees = productMapper.toTreeResource(product);
        List<ProductTree> result = institutionService.getProductsTree();
        Assertions.assertEquals(productTrees, result);
        verify(productService, times(1)).getProducts(false, true);
    }

    @Test
    void getProductsTreeEmptyProduct() {

        when(productService.getProducts(false, true)).thenReturn(Collections.emptyList());

        List<ProductTree> result = institutionService.getProductsTree();
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void updateInstitutionDescription() throws IOException {

        String institutionId = "institutionId";
        UpdateInstitutionResource updateInstitutionResource = new UpdateInstitutionResource();
        updateInstitutionResource.setDescription("updatedDescription");
        updateInstitutionResource.setDigitalAddress("updatedDigitalAddress");

        ClassPathResource pathResourceInstitution = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStreamInstitution;
        try {
            resourceStreamInstitution = Files.readAllBytes(pathResourceInstitution.getFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
        InstitutionResponse institutionResponse = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });

        when(coreInstitutionApiRestClient._updateInstitutionUsingPUT(institutionId, institutionMapper.toInstitutionPut(updateInstitutionResource)))
                .thenReturn(ResponseEntity.ok(institutionResponse));

        Institution result = institutionService.updateInstitutionDescription(institutionId, updateInstitutionResource);

        Assertions.assertEquals(institutionMapper.toInstitution(institutionResponse), result);
        verify(coreInstitutionApiRestClient, times(1))._updateInstitutionUsingPUT(institutionId, institutionMapper.toInstitutionPut(updateInstitutionResource));
    }

    @Test
    void updateInstitutionDescriptionWithoutInstitutionId() {

        UpdateInstitutionResource updateInstitutionResource = new UpdateInstitutionResource();
        updateInstitutionResource.setDescription("description");
        updateInstitutionResource.setDigitalAddress("digitalAddress");

        Assertions.assertThrows(IllegalArgumentException.class, () -> institutionService.updateInstitutionDescription(null, updateInstitutionResource));
    }

    @Test
    void updateInstitutionDescriptionWithoutUpdateInstitutionResource() {

        String institutionId = "institutionId";

        Assertions.assertThrows(IllegalArgumentException.class, () -> institutionService.updateInstitutionDescription(institutionId, null));
    }

    @Test
    void updateInstitutionDescriptionEmptyInstitution() {

        String institutionId = "institutionId";
        UpdateInstitutionResource updateInstitutionResource = new UpdateInstitutionResource();
        updateInstitutionResource.setDescription("updatedDescription");
        updateInstitutionResource.setDigitalAddress("updatedDigitalAddress");

        when(coreInstitutionApiRestClient._updateInstitutionUsingPUT(institutionId, institutionMapper.toInstitutionPut(updateInstitutionResource)))
                .thenReturn(ResponseEntity.ok(new InstitutionResponse()));

        Institution result = institutionService.updateInstitutionDescription(institutionId, updateInstitutionResource);
        Assertions.assertEquals(new Institution(), result);
        verify(coreInstitutionApiRestClient, times(1))._updateInstitutionUsingPUT(institutionId, institutionMapper.toInstitutionPut(updateInstitutionResource));
    }

    @Test
    void findInstitutionById() throws IOException {

        String institutionId = "123e4567-e89b-12d3-a456-426614174000";
        ProductGrantedAuthority productGrantedAuthority = new ProductGrantedAuthority(OPERATOR, "productRole", "productId");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(null,
                null,
                Collections.singletonList(new SelfCareGrantedAuthority("123e4567-e89b-12d3-a456-426614174000", Collections.singleton(productGrantedAuthority))));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ClassPathResource pathResourceInstitution = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStreamInstitution;
        try {
            resourceStreamInstitution = Files.readAllBytes(pathResourceInstitution.getFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
        InstitutionResponse institutionResponse = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionResponse));

        institutionService.findInstitutionById(institutionId);
        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
    }

    @Test
    void findInstitutionByIdWithLimitation() throws IOException {

        String institutionId = "123e4567-e89b-12d3-a456-426614174000";
        ProductGrantedAuthority productGrantedAuthority = new ProductGrantedAuthority(MANAGER, "productRole", "productId2");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(null,
                null,
                Collections.singletonList(new SelfCareGrantedAuthority("123e4567-e89b-12d3-a456-426614174000", Collections.singleton(productGrantedAuthority))));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ClassPathResource pathResourceInstitution = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStreamInstitution;
        try {
            resourceStreamInstitution = Files.readAllBytes(pathResourceInstitution.getFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
        InstitutionResponse institutionResponse = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionResponse));

        institutionService.findInstitutionById(institutionId);
        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
    }

    @Test
    void findInstitutionByIdWithoutMatch() throws IOException {

        String institutionId = "institutionId";
        ProductGrantedAuthority productGrantedAuthority = new ProductGrantedAuthority(MANAGER, "productRole", "productId");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(null,
                null,
                Collections.singletonList(new SelfCareGrantedAuthority("noMatchInstitutionId", Collections.singleton(productGrantedAuthority))));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ClassPathResource pathResourceInstitution = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStreamInstitution;
        try {
            resourceStreamInstitution = Files.readAllBytes(pathResourceInstitution.getFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource file", e);
        }
        InstitutionResponse institutionResponse = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionResponse));

        Institution result = institutionService.findInstitutionById(institutionId);
        Assertions.assertEquals(institutionMapper.toInstitution(institutionResponse), result);
        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
    }

    @Test
    void findInstitutionByIdWithoutInstitutionId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> institutionService.findInstitutionById(null));
    }

    @Test
    void findInstitutionByIdEmpty() {

        String institutionId = "institutionId";

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok().build());

        Institution result = institutionService.findInstitutionById(institutionId);
        Assertions.assertNull(result);
        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
    }
}