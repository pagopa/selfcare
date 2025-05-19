package it.pagopa.selfcare.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@QuarkusTest
public class UserPermissionServiceImplTest {

    @InjectMock
    UserInstitutionService userInstitutionService;

    @Inject
    UserPermissionService userPermissionService;

    @InjectMock
    it.pagopa.selfcare.product.service.ProductService productService;

    protected ObjectMapper objectMapper;

    private final String institutionId = UUID.randomUUID().toString();
    private final String productId = "prod-io";
    private final String productIdPremium = "prod-io-premium";
    private final String userId = "userId";

    @BeforeEach
    public void setUp() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper((new ObjectMapper()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).registerModule(new JavaTimeModule()));
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testHasPermissionWithAnyPermission() throws IOException {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.OPERATOR);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ANY))
                .thenReturn(Uni.createFrom().item(true));
        ClassPathResource productResource = new ClassPathResource("json/Product.json");
        byte[] resourceStreamInstitution = Files.readAllBytes(productResource.getFile().toPath());
        Product productIo = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });
        Mockito.when(productService.getProduct(productId)).thenReturn(productIo);
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ANY, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    void testHasPermissionWithADminPermissionOnlyWithProductId() throws IOException {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.OPERATOR);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, null, productId, PermissionTypeEnum.ANY))
                .thenReturn(Uni.createFrom().item(true));
        ClassPathResource productResource = new ClassPathResource("json/Product.json");
        byte[] resourceStreamInstitution = Files.readAllBytes(productResource.getFile().toPath());
        Product productIo = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });
        Mockito.when(productService.getProduct(productId)).thenReturn(productIo);

        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(null, productId, PermissionTypeEnum.ANY, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    void testHasPermissionWithValidPermission() throws IOException {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.MANAGER);
        product.setProductId(productIdPremium);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ADMIN))
                .thenReturn(Uni.createFrom().item(true));
        ClassPathResource productResource = new ClassPathResource("json/Product_prod-io-premium.json");
        byte[] resourceStreamInstitution = Files.readAllBytes(productResource.getFile().toPath());
        Product productIoPremium = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });
        Mockito.when(productService.getProduct(productIdPremium)).thenReturn(productIoPremium);
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productIdPremium, PermissionTypeEnum.ADMIN, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    void testHasPermissionWithInvalidPermission() throws IOException {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.OPERATOR);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ADMIN))
                .thenReturn(Uni.createFrom().item(false));
        ClassPathResource productResource = new ClassPathResource("json/Product.json");
        byte[] resourceStreamInstitution = Files.readAllBytes(productResource.getFile().toPath());
        Product productIo = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });
        Mockito.when(productService.getProduct(productId)).thenReturn(productIo);
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ADMIN, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(false);
    }

    @Test
    void testHasPermissionWithUserNotFound() throws IOException {
        // Arrange
        Mockito.when(userInstitutionService.existsValidUserProduct(eq(null), anyString(), anyString(), any(PermissionTypeEnum.class)))
                .thenReturn(Uni.createFrom().item(false));
        ClassPathResource productResource = new ClassPathResource("json/Product.json");
        byte[] resourceStreamInstitution = Files.readAllBytes(productResource.getFile().toPath());
        Product productIo = objectMapper.readValue(resourceStreamInstitution, new TypeReference<>() {
        });
        Mockito.when(productService.getProduct(productId)).thenReturn(productIo);
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ANY, userId);
        UniAssertSubscriber<Boolean> assertSubscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        assertSubscriber.assertCompleted().assertItem(false);
    }
}
