package it.pagopa.selfcare.user.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static it.pagopa.selfcare.user.model.constants.OnboardedProductState.*;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
public class UserPermissionServiceImplTest {

    @InjectMock
    UserInstitutionService userInstitutionService;

    @Inject
    UserPermissionService userPermissionService;

    private final String institutionId = UUID.randomUUID().toString();
    private final String productId = "prod-io";
    private final String userId = "userId";
    private static final List<OnboardedProductState> AVAILABLE_PRODUCT_STATES = List.of(ACTIVE, PENDING, TOBEVALIDATED);

    @Test
    void testHasPermissionWithAnyPermission() {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.OPERATOR);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ANY, AVAILABLE_PRODUCT_STATES))
                .thenReturn(Uni.createFrom().item(true));
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ANY, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    void testHasPermissionWithADminPermissionOnlyWithProductId() {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.OPERATOR);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, null, productId, PermissionTypeEnum.ANY, AVAILABLE_PRODUCT_STATES))
                .thenReturn(Uni.createFrom().item(true));

        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(null, productId, PermissionTypeEnum.ANY, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    void testHasPermissionWithValidPermission() {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.MANAGER);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ADMIN, AVAILABLE_PRODUCT_STATES))
                .thenReturn(Uni.createFrom().item(true));
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ADMIN, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    void testHasPermissionWithInvalidPermission() {
        // Arrange
        UserInstitution userInstitution = new UserInstitution();
        OnboardedProduct product = new OnboardedProduct();
        product.setRole(PartyRole.OPERATOR);
        product.setProductId(productId);
        userInstitution.setProducts(Collections.singletonList(product));
        Mockito.when(userInstitutionService.existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ADMIN, AVAILABLE_PRODUCT_STATES))
                .thenReturn(Uni.createFrom().item(false));
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ADMIN, userId);
        UniAssertSubscriber<Boolean> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        subscriber.assertCompleted().assertItem(false);
    }

    @Test
    void testHasPermissionWithUserNotFound() {
        // Arrange
        Mockito.when(userInstitutionService.existsValidUserProduct(eq(null), anyString(), anyString(), any(PermissionTypeEnum.class), eq(AVAILABLE_PRODUCT_STATES)))
                .thenReturn(Uni.createFrom().item(false));
        // Act
        Uni<Boolean> result = userPermissionService.hasPermission(institutionId, productId, PermissionTypeEnum.ANY, userId);
        UniAssertSubscriber<Boolean> assertSubscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert
        assertSubscriber.assertCompleted().assertItem(false);
    }
}
