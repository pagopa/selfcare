package it.pagopa.selfcare.user.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInstitution;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@QuarkusTest
public class UserPermissionServiceImplTest {

    @InjectMock
    UserInstitutionService userInstitutionService;

    @Inject
    UserPermissionService userPermissionService;

    private final String institutionId = UUID.randomUUID().toString();
    private final String productId = "prod-io";
    private final String userId = "userId";


    @Test
    public void testHasPermissionWithAnyPermission() {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.OPERATOR);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ANY))
                .thenReturn(Uni.createFrom().item(true));
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ANY, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    public void testHasPermissionWithADminPermissionOnlyWithProductId() {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.OPERATOR);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, null, productId, PermissionTypeEnum.ANY))
                .thenReturn(Uni.createFrom().item(true));
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(null, productId, PermissionTypeEnum.ANY, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    public void testHasPermissionWithValidPermission() {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.MANAGER);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ADMIN))
                .thenReturn(Uni.createFrom().item(true));
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ADMIN, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    public void testHasPermissionWithInvalidPermission() {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.OPERATOR);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ADMIN))
                .thenReturn(Uni.createFrom().item(false));
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ADMIN, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(false);
    }

    @Test
    public void testHasPermissionWithUserNotFound() {
        // Arrange
        Mockito.when(userInstitutionService.existsValidUserProduct(eq(null), anyString(), anyString(), any(PermissionTypeEnum.class)))
                .thenReturn(Uni.createFrom().item(false));
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ANY, userId);
        UniAssertSubscriber<Boolean> assertSubscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        assertSubscriber.assertCompleted().assertItem(false);
    }
}
