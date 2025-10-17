package it.pagopa.selfcare.iam.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;


@QuarkusTest
public class IamServiceImplTest {

  @Inject
  IamServiceImpl iamService;

  // @InjectMock
  // UserClaimsMapper mapper;


//  @Test
//  void shouldCreateUserSuccessfully(UniAsserter asserter) {
//    SaveUserRequest request = new SaveUserRequest();
//    request.setName("testUser");
//    request.setEmail("test@example.com");
//
//    UserClaims entity = spy(new UserClaims());
//    entity.setName("testUser");
//    entity.setEmail("test@example.com");
//
//    when(mapper.toEntity(request)).thenReturn(entity);
//    doAnswer(inv -> {
//      entity.setUid(UUID.randomUUID().toString());
//      return null;
//    }).when(entity).persistOrUpdate();
//
//    // Uni<UserClaims> uni = iamService.saveUser(request);
////
////    asserter.execute(() -> PanacheMock.mock(UserClaims.class));
////    asserter.execute(() -> when(UserClaims.persist(any(UserClaims.class), any()))
////      .thenAnswer(arg -> {
////        UserClaims userClaims = (UserClaims) arg.getArguments()[0];
////        userClaims.setUid(UUID.randomUUID().toString());
////        userClaims.setEmail(((UserClaims) arg.getArguments()[0]).getEmail());
////        return Uni.createFrom().nullItem();
////      }));
//
//    asserter.assertThat(() -> iamService.saveUser(request), result -> {
//      assertNotNull(result);
//      assertEquals("testUser", result.getName());
//      assertEquals("test@example.com", result.getEmail());
//      assertNotNull(result.getUid());
//    });
//
//
//    asserter.execute(() -> {
//      verify(mapper, times(2)).toEntity(request); // chiamato nella optional + replaceWith
//      verify(entity, times(1)).persistOrUpdate();
//    });
//  }

//  @Test
//  void shouldFailWhenUserIsNull() {
//    iamService.saveUser(null)
//      .subscribe().withSubscriber(UniAssertSubscriber.create())
//      .assertFailedWith(IllegalArgumentException.class, "User cannot be null");
//  }
//
//  @Test
//  void shouldFailWhenEmailIsNull() {
//    SaveUserRequest request = new SaveUserRequest();
//    request.setEmail(null);
//
//    iamService.saveUser(request)
//      .subscribe().withSubscriber(UniAssertSubscriber.create())
//      .assertFailedWith(IllegalArgumentException.class, "Email cannot be null");
//  }
//
//  @Test
//  void shouldFailWhenEmailIsBlank() {
//    SaveUserRequest request = new SaveUserRequest();
//    request.setEmail("  ");
//
//    iamService.saveUser(request)
//      .subscribe().withSubscriber(UniAssertSubscriber.create())
//      .assertFailedWith(IllegalArgumentException.class, "Email cannot be null");
//  }

 @Test
 @RunOnVertxContext
  void shouldFailWhenUserIsNull(UniAsserter asserter) {
    asserter.assertFailedWith(() -> iamService.saveUser(null),
      IllegalArgumentException.class
      // , ex -> assertEquals("User cannot be null", ex.getMessage())
    );
    // asserter.execute(() -> verifyNoInteractions(mapper));
  }

//  @Test
//  @RunOnVertxContext
//  void shouldFailWhenEmailIsNull(UniAsserter asserter) {
//    SaveUserRequest request = new SaveUserRequest();
//    request.setName("x");
//    request.setEmail(null);
//
//    asserter.assertFailedWith(
//      () -> iamService.saveUser(request),
//      IllegalArgumentException.class
////      , ex -> assertEquals("Email cannot be null", ex.getMessage())
//    );
//    asserter.execute(() -> verifyNoInteractions(mapper));
//  }

//  @Test
//  @RunOnVertxContext
//  void shouldFailWhenEmailIsBlank(UniAsserter asserter) {
//    SaveUserRequest request = new SaveUserRequest();
//    request.setName("x");
//    request.setEmail("   ");
//
//    asserter.assertFailedWith(
//      () -> iamService.saveUser(request),
//      IllegalArgumentException.class
//      // ,ex -> assertEquals("Email cannot be null", ex.getMessage())
//    );
//    asserter.execute(() -> verifyNoInteractions(mapper));
//  }

  // void mockPersistUserClaims(UniAsserter asserter) {
  //   asserter.execute(() -> PanacheMock.mock(UserClaims.class));
  //   asserter.execute(() -> when(UserClaims.persist(any(UserClaims.class), any()))
  //     .thenAnswer(arg -> {
  //       UserClaims userClaims = (UserClaims) arg.getArguments()[0];
  //       userClaims.setUid(UUID.randomUUID().toString());
  //       userClaims.setEmail(((UserClaims) arg.getArguments()[0]).getEmail());
  //       return Uni.createFrom().nullItem();
  //     }));
  // }
}
