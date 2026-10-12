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
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.ProductStatus;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.ProductMapper;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
        // given
        ProductOriginResponse response = new ProductOriginResponse();
        OriginResult mapped = new OriginResult();
        mapped.setOrigins(List.of());
        when(productApi.getProductOriginsById("prod-test", "AR")).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toOriginResult(response)).thenReturn(mapped);

        // when
        OriginResult result = productService.getOrigins("AR", "prod-test").await().indefinitely();

        // then
        assertSame(mapped, result);
        verify(productApi).getProductOriginsById("prod-test", "AR");
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getOrigins_sanitizesSpecialCharacters() {
        // given
        String raw = "<error>";
        ProductOriginResponse response = new ProductOriginResponse();
        OriginResult mapped = new OriginResult();
        mapped.setOrigins(List.of());
        when(productApi.getProductOriginsById(Encode.forJava(raw), "AR")).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toOriginResult(response)).thenReturn(mapped);

        // when
        OriginResult result = productService.getOrigins("AR", raw).await().indefinitely();

        // then
        assertNotNull(result);
        verify(productApi).getProductOriginsById(Encode.forJava(raw), "AR");
    }

    @Test
    void getOrigins_nullBodyThrows() {
        // given
        when(productApi.getProductOriginsById("test", "AR")).thenReturn(Uni.createFrom().nullItem());

        // when
        UniAssertSubscriber<OriginResult> result = productService.getOrigins("AR", "test")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertFailedWith(NullPointerException.class);
        verifyNoInteractions(productMapper);
    }

    @Test
    void getOrigins_nullOriginsListThrows() {
        // given
        ProductOriginResponse response = new ProductOriginResponse();
        when(productApi.getProductOriginsById("test", "AR")).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toOriginResult(response)).thenReturn(new OriginResult());

        // when
        UniAssertSubscriber<OriginResult> result = productService.getOrigins("AR", "test")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertFailedWith(NullPointerException.class);
    }

    @Test
    void getRequiredDocuments_mapsTypedParametersAndResult() {
        // given
        RequiredDocumentResponse dto = new RequiredDocumentResponse();
        dto.setId("doc-1");
        RequiredDocumentModel model = new RequiredDocumentModel();
        model.setId("doc-1");
        when(productApi.getRequiredDocuments("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(List.of(dto)));
        when(productMapper.toRequiredDocumentModelList(List.of(dto))).thenReturn(List.of(model));

        // when
        List<RequiredDocumentModel> result = productService.getRequiredDocuments("AR", "prod-test", "PA", "IPA").await().indefinitely();

        // then
        assertEquals(List.of(model), result);
        verify(productApi).getRequiredDocuments("prod-test", InstitutionType.PA, Origin.IPA, "AR");
        verifyNoMoreInteractions(productApi);
    }

    @Test
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void productLookupDoesNotMapBeforeTheDownstreamItem() {
        // given
        AtomicReference<UniEmitter<? super ProductResponse>> pending = new AtomicReference<>();
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(productApi.getProductById("prod-test", null))
                .thenReturn(Uni.createFrom().<ProductResponse>emitter(pending::set));
        when(productMapper.toProduct(response)).thenReturn(product);

        // when
        UniAssertSubscriber<Product> result = productService.getProduct("prod-test", null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(productMapper);
        pending.get().complete(response);
        result.assertCompleted().assertItem(product);
    }

    @Test
    void requiredDocumentsPropagateFailureWithoutMapping() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing documents");
        when(productApi.getRequiredDocuments("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<List<RequiredDocumentModel>> result =
                productService.getRequiredDocuments("AR", "prod-test", "PA", "IPA")
                        .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verifyNoInteractions(productMapper);
    }

    @Test
    void getRequiredDocuments_emptyList() {
        // given
        when(productApi.getRequiredDocuments("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(List.of()));
        when(productMapper.toRequiredDocumentModelList(List.of())).thenReturn(List.of());

        // when
        var actualAsync1 = productService.getRequiredDocuments("AR", "prod-test", "PA", "IPA").await().indefinitely();

        // then
        assertTrue(actualAsync1.isEmpty());
    }

    @Test
    void getRequiredDocuments_unknownInstitutionTypeIsRejectedWithoutCall() {
        // given
        // when
        assertThrows(IllegalArgumentException.class,
                () -> productService.getRequiredDocuments("AR", "prod-test", "pa", "IPA").await().indefinitely());
        assertThrows(IllegalArgumentException.class,
                () -> productService.getRequiredDocuments("AR", "prod-test", "PA", "ipa").await().indefinitely());
        // then
        verifyNoInteractions(productApi);
    }

    @Test
    void isRequiredDocumentsEnabled_readsHeader() {
        // given
        when(productApi.isRequiredDocumentsEnabled("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(responseWithFlag("true")));

        // when
        boolean enabled = productService.isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA")
                .await().indefinitely();

        // then
        assertTrue(enabled);
    }

    @Test
    void isRequiredDocumentsEnabled_falseHeader() {
        // given
        when(productApi.isRequiredDocumentsEnabled("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(responseWithFlag("false")));

        // when
        boolean enabled = productService.isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA")
                .await().indefinitely();

        // then
        assertFalse(enabled);
    }

    @Test
    void isRequiredDocumentsEnabled_missingHeaderIsFalse() {
        // given
        Response response = mock(Response.class);
        when(productApi.isRequiredDocumentsEnabled("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(response));

        // when
        boolean enabled = productService.isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA")
                .await().indefinitely();

        // then
        assertFalse(enabled);
        verify(response).close();
    }

    @Test
    void getProduct_delegatesTenantToHeader() {
        // given
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(productApi.getProductById("prod-io", null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(product);

        // when
        var actualAsync1 = productService.getProduct("prod-io", null).await().indefinitely();

        // then
        assertSame(product, actualAsync1);
        verify(productApi).getProductById("prod-io", null);
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getProduct_sanitizesSpecialCharacters() {
        // given
        String raw = "prod\"io\n";
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(productApi.getProductById(Encode.forJava(raw), null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(product);

        // when
        var actualAsync1 = productService.getProduct(raw, null).await().indefinitely();

        // then
        assertSame(product, actualAsync1);
    }

    @Test
    void getProduct_propagatesNotFound() {
        // given
        when(productApi.getProductById("missing", null))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("No product found with id missing")));

        // when
        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct("missing", null).await().indefinitely());

        // then
        verify(productApi).getProductById("missing", null);
        verifyNoInteractions(productMapper);
    }

    @Test
    void getProductValid_usesValidEndpoint() {
        // given
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        when(productApi.getValidProductById("prod-io", null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(product);

        // when
        var actualAsync1 = productService.getProductValid("prod-io").await().indefinitely();

        // then
        assertSame(product, actualAsync1);
        verify(productApi).getValidProductById("prod-io", null);
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void getProducts_requestsValidProductsAndKeepsOnlyActive() {
        // given
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

        // when
        List<Product> result = productService.getProducts(false).await().indefinitely();

        // then
        assertEquals(List.of(enabledActive, disabledActive), result);
        verify(productApi).getProducts(false, true, null);
    }

    @Test
    void getProducts_rootOnlyIsPropagated() {
        // given
        when(productApi.getProducts(true, true, null)).thenReturn(Uni.createFrom().item(List.of()));

        // when
        List<Product> result = productService.getProducts(true).await().indefinitely();

        // then
        assertTrue(result.isEmpty());
        verify(productApi).getProducts(true, true, null);
        verifyNoInteractions(productMapper);
    }

    @Test
    void getProducts_nullBodyThrows() {
        // given
        when(productApi.getProducts(false, true, null)).thenReturn(Uni.createFrom().nullItem());

        // when
        UniAssertSubscriber<List<Product>> result = productService.getProducts(false)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertFailedWith(NullPointerException.class);
    }

    @Test
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void originsDoNotWaitForTheDownstreamItem() {
        // given
        AtomicReference<UniEmitter<? super ProductOriginResponse>> pending = new AtomicReference<>();
        ProductOriginResponse response = new ProductOriginResponse();
        OriginResult mapped = new OriginResult();
        mapped.setOrigins(List.of());
        when(productApi.getProductOriginsById("prod-test", "AR"))
                .thenReturn(Uni.createFrom().<ProductOriginResponse>emitter(pending::set));
        when(productMapper.toOriginResult(response)).thenReturn(mapped);

        // when
        UniAssertSubscriber<OriginResult> result = productService.getOrigins("AR", "prod-test")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(productMapper);
        pending.get().complete(response);
        result.assertCompleted().assertItem(mapped);
        verify(productApi).getProductOriginsById("prod-test", "AR");
        verifyNoMoreInteractions(productApi);
    }

    @Test
    void enabledFlagClosesTheResponseWhenHeaderReadingFails() {
        // given
        Response response = mock(Response.class);
        IllegalStateException failure = new IllegalStateException("invalid header");
        when(response.getHeaderString(ProductServiceImpl.HEADER_REQUIRED_DOCUMENTS_ENABLED)).thenThrow(failure);
        when(productApi.isRequiredDocumentsEnabled("prod-test", InstitutionType.PA, Origin.IPA, "AR"))
                .thenReturn(Uni.createFrom().item(response));

        // when
        UniAssertSubscriber<Boolean> result = productService.isRequiredDocumentsEnabled("AR", "prod-test", "PA", "IPA")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verify(response).close();
    }

    @Test
    void listFailureIsPropagatedWithoutMappingOrRetry() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing catalog");
        when(productApi.getProducts(false, true, null)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<List<Product>> result = productService.getProducts(false)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verify(productApi).getProducts(false, true, null);
        verifyNoMoreInteractions(productApi);
        verifyNoInteractions(productMapper);
    }

    @Test
    void isProductEnabled_readsFeatureFromValidProduct() {
        // given
        ProductResponse enabledResponse = mock(ProductResponse.class);
        ProductResponse disabledResponse = mock(ProductResponse.class);
        when(productApi.getValidProductById("prod-io", null)).thenReturn(Uni.createFrom().item(enabledResponse));
        when(productApi.getValidProductById("prod-disabled", null)).thenReturn(Uni.createFrom().item(disabledResponse));
        when(productMapper.toProduct(enabledResponse)).thenReturn(product("prod-io", ProductStatus.ACTIVE, true));
        when(productMapper.toProduct(disabledResponse)).thenReturn(product("prod-disabled", ProductStatus.ACTIVE, false));

        // when
        var actualAsync1 = productService.isProductEnabled("prod-io").await().indefinitely();

        // then
        assertTrue(actualAsync1);
        var actualAsync2 = productService.isProductEnabled("prod-disabled").await().indefinitely();

        assertFalse(actualAsync2);
    }

    @Test
    void verifyAllowedByInstitutionTaxCode_ignoresCase() {
        // given
        ProductResponse response = new ProductResponse();
        Product product = new Product();
        product.setAllowedInstitutionTaxCode(List.of("ABC123"));
        when(productApi.getValidProductById("prod-io", null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(product);

        // when
        var actualAsync1 = productService.verifyAllowedByInstitutionTaxCode("prod-io", "abc123").await().indefinitely();

        // then
        assertTrue(actualAsync1);
    }

    @Test
    void verifyAllowedByInstitutionTaxCode_falseWhenNotListedOrListMissing() {
        // given
        ProductResponse response = new ProductResponse();
        Product listed = new Product();
        listed.setAllowedInstitutionTaxCode(List.of("ABC123"));
        when(productApi.getValidProductById("prod-io", null)).thenReturn(Uni.createFrom().item(response));
        when(productMapper.toProduct(response)).thenReturn(listed).thenReturn(new Product());

        // when
        var actualAsync1 = productService.verifyAllowedByInstitutionTaxCode("prod-io", "XYZ999").await().indefinitely();

        // then
        assertFalse(actualAsync1);
        var actualAsync2 = productService.verifyAllowedByInstitutionTaxCode("prod-io", "ABC123").await().indefinitely();

        assertFalse(actualAsync2);
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
