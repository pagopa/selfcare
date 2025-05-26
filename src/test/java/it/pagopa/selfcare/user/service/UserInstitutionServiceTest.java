package it.pagopa.selfcare.user.service;

import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class UserInstitutionServiceTest {

    @Inject
    private UserInstitutionServiceDefault userInstitutionService;

    @InjectMock
    private ProductService productServiceCacheable;

    @Test
    void findById() {
        final String id = new ObjectId().toHexString();
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        when(UserInstitution.findById(any()))
                .thenReturn(Uni.createFrom().item(userInstitution));
        Uni<UserInstitutionResponse> response = userInstitutionService.findById(id);
        Assertions.assertNotNull(response);
    }

    @Test
    public void checkExistsValidUserProductAnyWithoutProductId() {
        String userId = "userId";
        String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(0L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, institutionId, null, PermissionTypeEnum.ANY)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(false);
    }

    @Test
    public void checkExistProductIdByRoleAdminWithoutProductId() {
        String userId = "userId";
        String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, institutionId, null, PermissionTypeEnum.ADMIN)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(true);    }

    @Test
    public void checkExistProductIdByRoleAnyWithoutProductId() {
        String institutionId = "institutionId";
        String userId = "userId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, institutionId, null, PermissionTypeEnum.ANY)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    public void checkExistsValidUserProductAdminWithoutProductId() {
        String institutionId = "institutionId";
        String userId = "userId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(0L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, institutionId, null, PermissionTypeEnum.ADMIN)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(false);
    }

    @Test
    public void checkExistsValidUserProductAdminWithoutInstitutionId() {
        String userId = "userId";
        String productId = "productId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(0L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, null, productId, PermissionTypeEnum.ADMIN)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(false);
    }

    @Test
    public void checkExistsValidUserProductAnyWithoutInstitutionId() {
        String userId = "userId";
        String productId = "productId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(0L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, null, productId, PermissionTypeEnum.ANY)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(false);
    }

    @Test
    public void checkExistProductIdByRoleAdminWithoutInstitutionId() {
        String userId = "userId";
        String productId = "productId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, null, productId, PermissionTypeEnum.ADMIN)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(true);    }

    @Test
    public void checkExistProductIdByRoleAnyWithoutInstitutionId() {
        String userId = "userId";
        String productId = "productId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, null, productId, PermissionTypeEnum.ANY)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    public void checkExistsValidUserProductAdminWithInstitutionIdAndProductId() {
        String userId = "userId";
        String productId = "productId";
        String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(0L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ADMIN)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(false);
    }

    @Test
    public void checkExistsValidUserProductAnyWithInstitutionIdAndProductId() {
        String userId = "userId";
        String productId = "productId";
        String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(0L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ANY)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(false);
    }

    @Test
    public void checkExistProductIdByRoleAdminWithInstitutionIdAndProductId() {
        String userId = "userId";
        String productId = "productId";
        String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(1L));

        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ADMIN)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("productId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("role"));
        subscriber.assertCompleted().assertItem(true);    }

    @Test
    public void checkExistProductIdByRoleAnyWithInstitutionIdAndProductId() {
        String userId = "userId";
        String productId = "productId";
        String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);

        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Boolean> subscriber = userInstitutionService
                .existsValidUserProduct(userId, institutionId, productId, PermissionTypeEnum.ANY)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("institutionId"));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains("productId"));
        subscriber.assertCompleted().assertItem(true);
    }

    @Test
    void findByInstitutionId() {
        final String institutionId = "institutionId";
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find(any(), (Object) any()))
                .thenReturn(query);
        Uni<UserInstitutionResponse> response = userInstitutionService.findByInstitutionId(institutionId);
        Assertions.assertNotNull(response);
    }

    @Test
    void findByUserId() {
        final String userId = "userId";
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find(any(), (Object) any()))
                .thenReturn(query);
        Multi<UserInstitutionResponse> response = userInstitutionService.findByUserId(userId);
        Assertions.assertNotNull(response);
    }

    @Test
    void paginatedFindAllWithFilter() {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("institutionId", "institutionId");
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find((Document) any(), any()))
                .thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(userInstitution));
        AssertSubscriber<UserInstitution> subscriber = userInstitutionService.paginatedFindAllWithFilter(parameterMap, 0, 100)
                .subscribe().withSubscriber(AssertSubscriber.create(10));
        List<UserInstitution> response = subscriber.assertCompleted().getItems();
        Assertions.assertEquals(1, response.size());
    }


    @Test
    void retrieveFirstFilteredUserInstitution() {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("institutionId", "institutionId");
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find((Document) any(), any()))
                .thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        UniAssertSubscriber<UserInstitution> subscriber = userInstitutionService.retrieveFirstFilteredUserInstitution(parameterMap)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(userInstitution);
    }

    @Test
    void findAllWithFilter() {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("institutionId", "institutionId");
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find(any(Document.class), eq(null)))
                .thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(userInstitution));

        AssertSubscriber<UserInstitution> subscriber = userInstitutionService.findAllWithFilter(parameterMap)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        List<UserInstitution> actual = subscriber.assertCompleted().getItems();
        assertNotNull(actual);
        assertEquals(1, actual.size());
    }

    @Test
    void findAllAfterDate(){
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("institutionId", "institutionId");
        OffsetDateTime fromDate = OffsetDateTime.now();
        UserInstitution userInstitution = createDummyUserInstitution();
        OnboardedProduct onboardedProduct = createDummyOnboardedProduct();
        onboardedProduct.setCreatedAt(fromDate.plusDays(1));  // Ensure the product is created after the fromDate
        userInstitution.setProducts(List.of(onboardedProduct));

        PanacheMock.mock(UserInstitution.class); // Ensure PanacheMock is used to mock static methods

        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(UserInstitution.find(any(Document.class), eq(null))).thenReturn(query);  // Correct matcher usage
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(userInstitution));

        // Test the service method
        AssertSubscriber<UserInstitution> subscriber = userInstitutionService
                .findUserInstitutionsAfterDateWithFilter(parameterMap, fromDate)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        List<UserInstitution> actual = subscriber.assertCompleted().getItems();
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(userInstitution, actual.get(0));

        PanacheMock.verify(UserInstitution.class).find(any(Document.class), eq(null));
    }

    @Test
    void findAllAfterDate_noFilters(){
        Map<String, Object> parameterMap = new HashMap<>();
        OffsetDateTime fromDate = OffsetDateTime.now();
        UserInstitution userInstitution = createDummyUserInstitution();
        OnboardedProduct onboardedProduct = createDummyOnboardedProduct();
        onboardedProduct.setCreatedAt(fromDate.plusDays(1));  // Ensure the product is created after the fromDate
        userInstitution.setProducts(List.of(onboardedProduct));

        PanacheMock.mock(UserInstitution.class); // Ensure PanacheMock is used to mock static methods

        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(UserInstitution.find(any(Document.class), eq(null))).thenReturn(query);  // Correct matcher usage
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.stream()).thenReturn(Multi.createFrom().item(userInstitution));

        // Test the service method
        AssertSubscriber<UserInstitution> subscriber = userInstitutionService
                .findUserInstitutionsAfterDateWithFilter(parameterMap, fromDate)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        List<UserInstitution> actual = subscriber.assertCompleted().getItems();
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(userInstitution, actual.get(0));

        PanacheMock.verify(UserInstitution.class).find(any(Document.class), eq(null));

    }

    @Test
    void retrieveFilteredUserInstitution() {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("institutionId", "institutionId");
        List<UserInstitution> userInstitutionList = new ArrayList<>();
        UserInstitution userInstitution = createDummyUserInstitution();
        userInstitutionList.add(userInstitution);
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitutionList));
        when(UserInstitution.find((Document) any(), any()))
                .thenReturn(query);
        when(query.page(anyInt(), anyInt())).thenReturn(query);
        when(query.list()).thenReturn(Uni.createFrom().item(userInstitutionList));
        UniAssertSubscriber<List<UserInstitution>> subscriber = userInstitutionService.retrieveFilteredUserInstitution(parameterMap)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(userInstitutionList);
    }

    @Test
    void updateUserStatusToSuspendWithInstitutionAndOnboardedFilter() {
        final String userId = "userId";
        String institutionId = "institutionId";
        String productId = "productId";
        String productRole = "admin2";
        UserInstitution userInstitution = getUserInstitution(userId, institutionId, productId);

        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.find(any(Document.class), any())).thenReturn(query);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update(any(Document.class)))
                .thenReturn(update);
        when(update.where(embeddedCaptor.capture())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(userId, institutionId, productId, null, productRole, OnboardedProductState.SUSPENDED)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains(OnboardedProductState.ACTIVE.name()));
        Assertions.assertFalse(embeddedCaptor.getValue().toString().contains(OnboardedProductState.SUSPENDED.name()));
    }

    @Test
    void updateUserStatusToActiveWithProductRoleAlreadyActive() {
        final String userId = "userId";
        String institutionId = "institutionId";
        String productId = "productId";
        String productRole = "admin2";

        UserInstitution userInstitution = getUserInstitution(userId, institutionId, productId);

        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(UserInstitution.find(any(Document.class), any())).thenReturn(query);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(userId, institutionId, productId, null, productRole, OnboardedProductState.ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(0L);
    }

    @Test
    void updateUserStatusToDeleteWithInstitutionAndOnboardedFilter() {
        final String userId = "userId";
        String institutionId = "institutionId";
        String productId = "productId";
        String productRole = "admin2";
        UserInstitution userInstitution = getUserInstitution(userId, institutionId, productId);

        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);
        when(UserInstitution.find(any(Document.class), any())).thenReturn(query);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update(any(Document.class)))
                .thenReturn(update);
        when(update.where(embeddedCaptor.capture())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(userId, institutionId, productId, null, productRole, OnboardedProductState.DELETED)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains(OnboardedProductState.ACTIVE.name()));
        Assertions.assertTrue(embeddedCaptor.getValue().toString().contains(OnboardedProductState.SUSPENDED.name()));
    }

    @Test
    void updateUserStatusToActiveWithInstitutionAndOnboardedFilter() {
        final String userId = "userId";
        String institutionId = "institutionId";
        String productId = "productId";
        String productRole = "admin2";
        UserInstitution userInstitution = getUserInstitution(userId, institutionId, productId);
        userInstitution.getProducts().forEach(onboardedProduct -> onboardedProduct.setStatus(OnboardedProductState.SUSPENDED));

        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(UserInstitution.find(any(Document.class), any())).thenReturn(query);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update(any(Document.class)))
                .thenReturn(update);
        when(update.where(any())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(userId, institutionId, productId, null, productRole, OnboardedProductState.ACTIVE)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void updateUserStatusWithInstitutionAndOnboardedFilter() {
        final String userId = "userId";
        String institutionId = "institutionId";
        String productId = "productId";
        String productRole = "admin2";
        UserInstitution userInstitution = getUserInstitution(userId, institutionId, productId);
        when(productServiceCacheable.validateProductRole(eq(productId), eq(productRole), any())).thenReturn(new ProductRole());
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(UserInstitution.find(any(Document.class), any())).thenReturn(query);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update(any(Document.class)))
                .thenReturn(update);
        when(update.where(any())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateUserStatusWithOptionalFilterByInstitutionAndProduct(userId, institutionId, productId, null, productRole, OnboardedProductState.REJECTED)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    private static UserInstitution getUserInstitution(String userId, String institutionId, String productId) {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId(userId);
        userInstitution.setInstitutionId(institutionId);

        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId(productId);
        onboardedProduct.setStatus(OnboardedProductState.ACTIVE);
        onboardedProduct.setRole(PartyRole.OPERATOR);
        onboardedProduct.setProductRole("admin2");

        OnboardedProduct onboardedProduct2 = new OnboardedProduct();
        onboardedProduct2.setProductId(productId);
        onboardedProduct2.setStatus(OnboardedProductState.SUSPENDED);
        onboardedProduct2.setRole(PartyRole.OPERATOR);
        onboardedProduct2.setProductRole("admin2");

        userInstitution.setProducts(List.of(onboardedProduct2, onboardedProduct));
        return userInstitution;
    }

    @Test
    void deleteUserInstitutionProduct() {
        final String userId = "userId";
        String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update(any(Document.class)))
                .thenReturn(update);
        when(update.where(any())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.deleteUserInstitutionProduct(userId, institutionId, "productID")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void deleteUserInstitutionProductUsers() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        PanacheMock.mock(UserInstitution.class);

        final UpdateResult updateResult = Mockito.mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);

        final ReactiveMongoCollection mongoCollection = Mockito.mock(ReactiveMongoCollection.class);
        when(mongoCollection.updateMany(any(Document.class), any(Document.class), any(UpdateOptions.class))).thenReturn(Uni.createFrom().item(updateResult));
        when(UserInstitution.mongoCollection()).thenReturn(mongoCollection);

        UniAssertSubscriber<Long> subscriber = userInstitutionService.deleteUserInstitutionProductUsers(institutionId, productId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void updateUserProductCreatedAt() {
        final String userId = "userId";
        final String institutionId = "institutionId";
        final String productId = "productId";
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update(any(Document.class)))
                .thenReturn(update);
        when(update.where(any())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateUserCreatedAtByInstitutionAndProduct(institutionId, List.of(userId), productId, OffsetDateTime.now())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void findByUserIdAndInstitutionId() {
        final String userId = "userId";
        final String institutionId = "institutionId";
        UserInstitution userInstitution = createDummyUserInstitution();
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().item(userInstitution));
        when(UserInstitution.find(any(Document.class), eq(null)))
                .thenReturn(query);
        UniAssertSubscriber<UserInstitution> subscriber = userInstitutionService.findByUserIdAndInstitutionId(userId, institutionId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UserInstitution actual = subscriber.assertCompleted().awaitItem().getItem();
        Assertions.assertNotNull(actual);
    }

    @Test
    void findByUserIdAndInstitutionId_WithNoResult() {
        final String userId = "userId";
        final String institutionId = "institutionId";
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheQuery query = Mockito.mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());
        when(UserInstitution.find(any(Document.class), eq(null)))
                .thenReturn(query);
        UniAssertSubscriber<UserInstitution> subscriber = userInstitutionService.findByUserIdAndInstitutionId(userId, institutionId)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UserInstitution actual = subscriber.assertCompleted().awaitItem().getItem();
        Assertions.assertNull(actual);
    }

    @Test
    void updateInstitutionDescription() {
        final String institutionId = "institutionId";
        UpdateDescriptionDto updateDescriptionDto = new UpdateDescriptionDto();
        String description = "description";
        String rootName = "rootName";
        updateDescriptionDto.setInstitutionDescription(description);
        updateDescriptionDto.setInstitutionRootName(rootName);
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update(any(Document.class))).thenReturn(update);
        when(update.where(any())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateInstitutionDescription(institutionId, updateDescriptionDto)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void updateInstitutionDescription_noInstitutionRootName() {
        final String institutionId = "institutionId";
        UpdateDescriptionDto updateDescriptionDto = new UpdateDescriptionDto();
        String description = "description";
        updateDescriptionDto.setInstitutionDescription(description);
        PanacheMock.mock(UserInstitution.class);
        ReactivePanacheUpdate update = Mockito.mock(ReactivePanacheUpdate.class);
        when(UserInstitution.update(any(Document.class))).thenReturn(update);
        when(update.where(any())).thenReturn(Uni.createFrom().item(1L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService.updateInstitutionDescription(institutionId, updateDescriptionDto)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(1L);
    }

    @Test
    void countUsers() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<PartyRole> roles = List.of(PartyRole.MANAGER, PartyRole.DELEGATE);
        final List<OnboardedProductState> status = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING);

        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);

        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(2L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService
                .countUsers(institutionId, productId, roles, status)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        final String docString = embeddedCaptor.getValue().toString();
        Assertions.assertTrue(docString.contains("institutionId"));
        Assertions.assertTrue(docString.contains("productId"));
        Assertions.assertTrue(docString.contains("role"));
        Assertions.assertTrue(docString.contains("MANAGER"));
        Assertions.assertTrue(docString.contains("DELEGATE"));
        Assertions.assertTrue(docString.contains("status"));
        Assertions.assertTrue(docString.contains("ACTIVE"));
        Assertions.assertTrue(docString.contains("PENDING"));
        subscriber.assertCompleted().assertItem(2L);
    }

    @Test
    void countUsersWithoutRolesAndStatus() {
        final String institutionId = "institutionId";
        final String productId = "productId";

        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);

        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(4L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService
                .countUsers(institutionId, productId, null, null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        final String docString = embeddedCaptor.getValue().toString();
        Assertions.assertTrue(docString.contains("institutionId"));
        Assertions.assertTrue(docString.contains("productId"));
        Assertions.assertFalse(docString.contains("status"));
        Assertions.assertFalse(docString.contains("role"));
        subscriber.assertCompleted().assertItem(4L);
    }

    @Test
    void countUsersWithRolesAndNoStatus() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<PartyRole> roles = List.of(PartyRole.MANAGER, PartyRole.DELEGATE);
        final List<OnboardedProductState> status = new ArrayList<>();

        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);

        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(8L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService
                .countUsers(institutionId, productId, roles, status)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        final String docString = embeddedCaptor.getValue().toString();
        Assertions.assertTrue(docString.contains("institutionId"));
        Assertions.assertTrue(docString.contains("productId"));
        Assertions.assertTrue(docString.contains("role"));
        Assertions.assertTrue(docString.contains("MANAGER"));
        Assertions.assertTrue(docString.contains("DELEGATE"));
        Assertions.assertFalse(docString.contains("status"));
        subscriber.assertCompleted().assertItem(8L);
    }

    @Test
    void countUsersWithStatusAndNoRoles() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<PartyRole> roles = new ArrayList<>();
        final List<OnboardedProductState> status = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING);

        PanacheMock.mock(UserInstitution.class);
        ArgumentCaptor<Document> embeddedCaptor = ArgumentCaptor.forClass(Document.class);

        when(UserInstitution.count(embeddedCaptor.capture()))
                .thenReturn(Uni.createFrom().item(16L));
        UniAssertSubscriber<Long> subscriber = userInstitutionService
                .countUsers(institutionId, productId, roles, status)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        final String docString = embeddedCaptor.getValue().toString();
        Assertions.assertTrue(docString.contains("institutionId"));
        Assertions.assertTrue(docString.contains("productId"));
        Assertions.assertTrue(docString.contains("status"));
        Assertions.assertTrue(docString.contains("ACTIVE"));
        Assertions.assertTrue(docString.contains("PENDING"));
        Assertions.assertFalse(docString.contains("role"));
        subscriber.assertCompleted().assertItem(16L);
    }

    private UserInstitution createDummyUserInstitution() {
        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setId(ObjectId.get());
        userInstitution.setUserId("userId");
        userInstitution.setInstitutionId("institutionId");
        userInstitution.setInstitutionRootName("institutionRootName");
        return userInstitution;
    }
    private OnboardedProduct createDummyOnboardedProduct(){
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId("onboardedProductId");
        return onboardedProduct;
    }


}
