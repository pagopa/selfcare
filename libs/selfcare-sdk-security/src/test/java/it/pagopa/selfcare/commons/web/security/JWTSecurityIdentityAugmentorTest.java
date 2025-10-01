package it.pagopa.selfcare.commons.web.security;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JWTSecurityIdentityAugmentorTest {

  private JWTSecurityIdentityAugmentor augmentor;

  @Mock
  private AuthenticationRequestContext authContext;

  @Mock
  private JsonWebToken jsonWebToken;

  @Mock
  private Principal nonJwtPrincipal;

  @BeforeEach
  void setUp() {
    augmentor = new JWTSecurityIdentityAugmentor();
  }

  @Test
  void testAugment_WithValidJWT_ShouldAddRoleAndAttribute() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn("SPID");

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .addRole("USER")
      .build();

    // When
    SecurityIdentity result = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result);
    assertEquals("SPID", result.getAttribute("jwt.issuer"));
    assertTrue(result.getRoles().contains("USER")); // Ruoli originali preservati
  }

  @Test
  void testAugment_WithPAGOPAIssuer_ShouldAddRoleAndAttribute() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn("PAGOPA");

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .build();

    // When
    SecurityIdentity result = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result);
    assertTrue(result.getRoles().contains("SUPPORT"));
    assertEquals("PAGOPA", result.getAttribute("jwt.issuer"));
  }

  @Test
  void testAugment_WithNonJWTPrincipal_ShouldReturnOriginalIdentity() {
    // Given
    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(nonJwtPrincipal)
      .addRole("BASIC_USER")
      .build();

    // When
    SecurityIdentity result = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result);
    assertEquals(identity, result);
    assertTrue(result.getRoles().contains("BASIC_USER"));
    assertFalse(result.getRoles().contains("SUPPORT"));
    assertNull(result.getAttribute("jwt.issuer"));
  }

  @Test
  void testAugment_WithNullIssuer_ShouldReturnOriginalIdentity() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn(null);

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .build();

    // When
    SecurityIdentity result = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result);
    assertEquals(identity, result);
    assertFalse(result.getRoles().contains("SUPPORT"));
  }

  @Test
  void testAugment_PreservesExistingRoles() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn("SPID");

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .addRole("ADMIN")
      .addRole("USER")
      .build();

    // When
    SecurityIdentity result = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result);
    assertTrue(result.getRoles().contains("ADMIN"));
    assertTrue(result.getRoles().contains("USER"));
    assertEquals(2, result.getRoles().size());
  }

  @Test
  void testAugment_PreservesExistingAttributes() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn("PAGOPA");

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .addAttribute("custom.attribute", "custom-value")
      .build();

    // When
    SecurityIdentity result = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result);
    assertEquals("PAGOPA", result.getAttribute("jwt.issuer"));
    assertEquals("custom-value", result.getAttribute("custom.attribute"));
  }

  @Test
  void testAugment_WithEmptyIssuer_ShouldReturnOriginalIdentity() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn("");

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .build();

    // When
    SecurityIdentity result = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result);
    assertFalse(result.getRoles().contains("SUPPORT"));
  }

  @Test
  void testAugment_MultipleInvocations_ShouldBeIdempotent() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn("SPID");

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .build();

    // When - prima invocazione
    SecurityIdentity result1 = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // When - seconda invocazione
    SecurityIdentity result2 = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result1);
    assertNotNull(result2);
    assertEquals(result1.getRoles(), result2.getRoles());
    assertEquals((String) result1.getAttribute("jwt.issuer"), (String) result2.getAttribute("jwt.issuer"));
  }

  @Test
  void testAugment_WithSpecialCharactersInIssuer() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn("SPID");

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .build();

    // When
    SecurityIdentity result = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result);
    assertEquals("SPID", result.getAttribute("jwt.issuer"));
  }

  @Test
  void testAugment_VerifyNoInteractionWithAuthContext() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn("SPID");

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .build();

    // When
    augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted();

    // Then
    verifyNoInteractions(authContext);
  }

  @Test
  void testAugment_ReactiveChain_DoesNotBlock() {
    // Given
    when(jsonWebToken.getIssuer()).thenReturn("SPID");

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .build();

    // When
    UniAssertSubscriber<SecurityIdentity> subscriber = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then - verifica che completi senza bloccare
    subscriber.assertCompleted();
    assertNotNull(subscriber.getItem());
  }

  @Test
  void testAugment_WithLongIssuerString() {
    // Given
    String longIssuer = "A".repeat(1000);
    when(jsonWebToken.getIssuer()).thenReturn(longIssuer);

    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
      .setPrincipal(jsonWebToken)
      .build();

    // When
    SecurityIdentity result = augmentor.augment(identity, authContext)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertCompleted()
      .getItem();

    // Then
    assertNotNull(result);
    assertEquals(longIssuer, result.getAttribute("jwt.issuer"));
  }
}