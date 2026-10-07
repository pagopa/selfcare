package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.ProductStatus;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.ProductMapper;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.owasp.encoder.Encode;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductApi productApi;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getOrigins_delegatesWithSanitizedIdAndTenant() {
        ProductOriginResponse response = new ProductOriginResponse();
        OriginResult mapped = new OriginResult();
        mapped.setOrigins(List.of());
        when(productApi.getProductOriginsById("prod-test", "AR")).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toOriginResult(response)).thenReturn(mapped);

        OriginResult result = productService.getOrigins("AR", "prod-test");

        assertSame(mapped, result);
        verify(productApi).getProductOriginsById("prod-test", "AR");
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getOrigins_sanitizesSpecialCharacters() {
        String raw = "<error>";
        ProductOriginResponse response = new ProductOriginResponse();
        OriginResult mapped = new OriginResult();
        mapped.setOrigins(List.of());
        when(productApi.getProductOriginsById(Encode.forJava(raw), "AR")).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toOriginResult(response)).thenReturn(mapped);

        assertNotNull(productService.getOrigins("AR", raw));
        verify(productApi).getProductOriginsById(Encode.forJava(raw), "AR");
    }

    @Test
    void getOrigins_nullBodyThrows() {
        when(productApi.getProductOriginsById("test", "AR")).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NullPointerException.class, () -> productService.getOrigins("AR", "test"));
        verifyNoInteractions(productMapper);
    }

    @Test
    void getOrigins_nullOriginsListThrows() {
        ProductOriginResponse response = new ProductOriginResponse();
        when(productApi.getProductOriginsById("test", "AR")).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toOriginResult(response)).thenReturn(new OriginResult());

        assertThrows(NullPointerException.class, () -> productService.getOrigins("AR", "test"));
    }

    @Test
    void getRequiredDocuments_mapsTypedParametersAndResult() {
        RequiredDocumentResponse dto = new RequiredDocumentResponse();
        dto.setId("doc-1");
        RequiredDocumentModel model = new RequiredDocumentModel();
        model.setId("doc-1");
        when(productApi.getRequiredDocuments("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(List.of(dto)));
        when(productMapper.toRequiredDocumentModelList(List.of(dto))).thenReturn(List.of(model));

        List<RequiredDocumentModel> result = productService.getRequiredDocuments("AR", "prod-test", "PA", "IPA");

        assertEquals(List.of(model), result);
        verify(productApi).getRequiredDocuments("prod-test", InstitutionType.PA, Origin.IPA, "AR");
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getRequiredDocuments_emptyList() {
        when(productApi.getRequiredDocuments("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(List.of()));
        when(productMapper.toRequiredDocumentModelList(List.of())).thenReturn(List.of());

        assertTrue(productService.getRequiredDocuments("AR", "prod-test", "PA", "IPA").isEmpty());
    }

    @Test
    void getRequiredDocuments_unknownInstitutionTypeIsRejectedWithoutCall() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getRequiredDocuments("AR", "prod-test", "pa", "IPA"));
        assertThrows(IllegalArgumentException.class,
                () -> productService.getRequiredDocuments("AR", "prod-test", "PA", "ipa"));
        verifyNoInteractions(productApi);
    }

    @Test
    void isRequiredDocumentsEnabled_readsHeader() {
        when(productApi.isRequiredDocumentsEnabled("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(responseWithFlag("true")));

        assertTrue(productService.isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA"));
    }

    @Test
    void isRequiredDocumentsEnabled_falseHeader() {
        when(productApi.isRequiredDocumentsEnabled("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(responseWithFlag("false")));

        assertFalse(productService.isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA"));
    }

    @Test
    void isRequiredDocumentsEnabled_missingHeaderIsFalse() {
        Response response = mock(Response.class);
        when(productApi.isRequiredDocumentsEnabled("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(response));

        assertFalse(productService.isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA"));
        verify(response).close();
    }

    @Test
    void getProduct_delegatesTenantToHeader() {
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(productApi.getProductById("prod-io", null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(product);

        assertSame(product, productService.getProduct("prod-io", null));
        verify(productApi).getProductById("prod-io", null);
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getProduct_sanitizesSpecialCharacters() {
        String raw = "prod\"io\n";
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(productApi.getProductById(Encode.forJava(raw), null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(product);

        assertSame(product, productService.getProduct(raw, null));
    }

    @Test
    void getProduct_propagatesNotFound() {
        when(productApi.getProductById("missing", null))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("No product found with id missing")));

        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct("missing", null));
    }

    @Test
    void getProductValid_usesValidEndpoint() {
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(productApi.getValidProductById("prod-io", null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(product);

        assertSame(product, productService.getProductValid("prod-io"));
        verify(productApi).getValidProductById("prod-io", null);
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getProducts_requestsValidProductsAndKeepsOnlyActiveAndEnabled() {
        ProductResponse r1 = mock(ProductResponse.class);
        ProductResponse r2 = mock(ProductResponse.class);
        ProductResponse r3 = mock(ProductResponse.class);
        Product enabledActive = product("enabled-active", ProductStatus.ACTIVE, true);
        Product disabledActive = product("disabled-active", ProductStatus.ACTIVE, false);
        Product enabledTesting = product("enabled-testing", ProductStatus.TESTING, true);
        when(productApi.getProducts(false, true, null)).thenReturn(Uni.createFrom().item(List.of(r1, r2, r3)));
        when(productMapper.toProduct(r1)).thenReturn(enabledActive);
        when(productMapper.toProduct(r2)).thenReturn(disabledActive);
        when(productMapper.toProduct(r3)).thenReturn(enabledTesting);

        assertEquals(List.of(enabledActive), productService.getProducts(false));
        verify(productApi).getProducts(false, true, null);
    }

    @Test
    void getProducts_rootOnlyIsPropagated() {
        when(productApi.getProducts(true, true, null)).thenReturn(Uni.createFrom().item(List.of()));

        assertTrue(productService.getProducts(true).isEmpty());
        verify(productApi).getProducts(true, true, null);
        verifyNoInteractions(productMapper);
    }

    @Test
    void getProducts_nullBodyThrows() {
        when(productApi.getProducts(false, true, null)).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NullPointerException.class, () -> productService.getProducts(false));
    }

    @Test
    void isProductEnabled_readsFeatureFromValidProduct() {
        ProductResponse enabledResponse = mock(ProductResponse.class);
        ProductResponse disabledResponse = mock(ProductResponse.class);
        when(productApi.getValidProductById("prod-io", null)).thenReturn(Uni.createFrom().item(enabledResponse));
        when(productApi.getValidProductById("prod-disabled", null)).thenReturn(Uni.createFrom().item(disabledResponse));
        when(productMapper.toProduct(enabledResponse)).thenReturn(product("prod-io", ProductStatus.ACTIVE, true));
        when(productMapper.toProduct(disabledResponse)).thenReturn(product("prod-disabled", ProductStatus.ACTIVE, false));

        assertTrue(productService.isProductEnabled("prod-io"));
        assertFalse(productService.isProductEnabled("prod-disabled"));
    }

    @Test
    void verifyAllowedByInstitutionTaxCode_ignoresCase() {
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        product.setAllowedInstitutionTaxCode(List.of("ABC123"));
        when(productApi.getValidProductById("prod-io", null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(product);

        assertTrue(productService.verifyAllowedByInstitutionTaxCode("prod-io", "abc123"));
    }

    @Test
    void verifyAllowedByInstitutionTaxCode_falseWhenNotListedOrListMissing() {
        ProductResponse response = new ProductResponse();
        Product listed = new Product();
        listed.setAllowedInstitutionTaxCode(List.of("ABC123"));
        when(productApi.getValidProductById("prod-io", null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(listed).thenReturn(new Product());

        assertFalse(productService.verifyAllowedByInstitutionTaxCode("prod-io", "XYZ999"));
        assertFalse(productService.verifyAllowedByInstitutionTaxCode("prod-io", "ABC123"));
    }

    private static Product product(String id, ProductStatus status, boolean enabled) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        product.setEnabled(enabled);
        return product;
    }

    private static Response responseWithFlag(String value) {
        return Response.ok().header(ProductServiceImpl.HEADER_REQUIRED_DOCUMENTS_ENABLED, value).build();
    }
}
