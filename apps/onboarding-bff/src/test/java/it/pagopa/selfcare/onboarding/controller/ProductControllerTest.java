package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.model.ContractTemplate;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.model.dto.response.ProductResource;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.service.ProductService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @InjectMocks
    private ProductController controller;

    @Mock
    private ProductService productService;

    @Mock
    private InstitutionMapper productMapper;

    @Test
    void getProduct_mapsTheServiceResult() {
        Product product = new Product();
        ProductResource resource = new ProductResource();
        when(productService.getProduct("prod-io", InstitutionType.PA)).thenReturn(product);
        when(productMapper.toResource(product)).thenReturn(resource);

        assertSame(resource, controller.getProduct("prod-io", "PA"));
    }

    @Test
    void getProduct_withoutInstitutionTypePassesNull() {
        Product product = new Product();
        when(productService.getProduct("prod-io", null)).thenReturn(product);
        when(productMapper.toResource(product)).thenReturn(new ProductResource());

        controller.getProduct("prod-io", null);
        controller.getProduct("prod-io", " ");

        verify(productService, org.mockito.Mockito.times(2)).getProduct("prod-io", null);
    }

    @Test
    void getProduct_unknownProductIsReportedWithTheSpringMessage() {
        when(productService.getProduct("missing", null)).thenThrow(new ResourceNotFoundException("{\"detail\":\"x\"}"));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> controller.getProduct("missing", null));

        assertEquals("No product found with id missing", exception.getMessage());
    }

    @Test
    void getProduct_invalidInstitutionTypeIsABadRequestWithoutDownstreamCall() {
        assertThrows(InvalidRequestException.class, () -> controller.getProduct("prod-io", "NOT_A_TYPE"));

        verifyNoInteractions(productService);
    }

    @Test
    void getProducts_returnsTheActiveProductsOfAnyLevel() {
        // given
        Product product = new Product();
        ProductResource resource = new ProductResource();
        when(productService.getProducts(false)).thenReturn(Uni.createFrom().item(List.of(product)));
        when(productMapper.toResource(product)).thenReturn(resource);

        // when
        List<ProductResource> result = controller.getProducts().await().indefinitely();

        // then
        assertEquals(List.of(resource), result);
    }

    @Test
    void getProductsAdmin_keepsRootProductsWithADefaultUserContractTemplate() {
        // given
        Product withTemplate = productWithUserTemplate("DEFAULT", "path/to/template");
        Product withoutPath = productWithUserTemplate("DEFAULT", null);
        Product withoutMappings = new Product();
        ProductResource resource = new ProductResource();
        when(productService.getProducts(true)).thenReturn(Uni.createFrom().item(List.of(withTemplate, withoutPath, withoutMappings)));
        when(productMapper.toResource(withTemplate)).thenReturn(resource);

        // when
        List<ProductResource> result = controller.getProductsAdmin().await().indefinitely();

        // then
        assertEquals(List.of(resource), result);
    }

    @Test
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void listMappingWaitsForTheItemWithoutBlocking() {
        // given
        AtomicReference<UniEmitter<? super List<Product>>> pending = new AtomicReference<>();
        Product product = new Product();
        ProductResource resource = new ProductResource();
        when(productService.getProducts(false)).thenReturn(Uni.createFrom().<List<Product>>emitter(pending::set));
        when(productMapper.toResource(product)).thenReturn(resource);

        // when
        UniAssertSubscriber<List<ProductResource>> result = controller.getProducts()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(productMapper);
        pending.get().complete(List.of(product));
        result.assertCompleted().assertItem(List.of(resource));
    }

    @Test
    void emptyListRemainsAnEmptyList() {
        // given
        when(productService.getProducts(false)).thenReturn(Uni.createFrom().item(List.of()));

        // when
        List<ProductResource> result = controller.getProducts().await().indefinitely();

        // then
        assertEquals(List.of(), result);
        verifyNoInteractions(productMapper);
    }

    @Test
    void listFailureIsPropagatedWithoutMapping() {
        // given
        ResourceNotFoundException failure = new ResourceNotFoundException("missing catalog");
        when(productService.getProducts(false)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<List<ProductResource>> result = controller.getProducts()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verifyNoInteractions(productMapper);
    }

    private static Product productWithUserTemplate(String institutionType, String path) {
        ContractTemplate template = new ContractTemplate();
        template.setContractTemplatePath(path);
        Product product = new Product();
        product.setUserContractMappings(Map.of(institutionType, template));
        return product;
    }
}
