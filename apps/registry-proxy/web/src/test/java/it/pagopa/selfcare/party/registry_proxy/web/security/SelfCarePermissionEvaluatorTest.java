package it.pagopa.selfcare.party.registry_proxy.web.security;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SelfCarePermissionEvaluatorTest {

  @InjectMocks
  private SelfCarePermissionEvaluator permissionEvaluator;

  @Mock
  private Authentication authentication;

  @Mock
  private SelfCareUser selfCareUser;

  private static final String ISSUER = "test-issuer";
  private static final String DIFFERENT_ISSUER = "different-issuer";
  private static final String PERMISSION = "READ";

  @Test
  void hasPermission_shouldReturnTrue_whenIssuerMatches() {
    // Given
    when(selfCareUser.getIssuer()).thenReturn(ISSUER);
    FilterAuthorityDomain filterAuthorityDomain = new FilterAuthorityDomain(ISSUER);
    when(authentication.getPrincipal()).thenReturn(selfCareUser);

    // When
    boolean result = permissionEvaluator.hasPermission(authentication, filterAuthorityDomain, PERMISSION);

    // Then
    assertTrue(result);
    verify(authentication).getPrincipal();
    verify(selfCareUser).getIssuer();
  }

  @Test
  void hasPermission_shouldReturnFalse_whenIssuerDoesNotMatch() {
    // Given
    when(authentication.getPrincipal()).thenReturn(selfCareUser);
    when(selfCareUser.getIssuer()).thenReturn(ISSUER);
    FilterAuthorityDomain filterAuthorityDomain = new FilterAuthorityDomain(DIFFERENT_ISSUER);

    // When
    boolean result = permissionEvaluator.hasPermission(authentication, filterAuthorityDomain, PERMISSION);

    // Then
    assertFalse(result);
    verify(authentication).getPrincipal();
    verify(selfCareUser).getIssuer();
  }

  @Test
  void hasPermission_shouldReturnFalse_whenTargetDomainObjectIsNotFilterAuthorityDomain() {
    // Given
    when(authentication.getPrincipal()).thenReturn(selfCareUser);
    when(selfCareUser.getIssuer()).thenReturn(ISSUER);
    Object targetDomainObject = new Object();

    // When
    boolean result = permissionEvaluator.hasPermission(authentication, targetDomainObject, PERMISSION);

    // Then
    assertFalse(result);
    verify(authentication).getPrincipal();
    verify(selfCareUser).getIssuer();
  }

  @Test
  void hasPermission_shouldReturnFalse_whenTargetDomainObjectIsNull() {
    // Given
    when(selfCareUser.getIssuer()).thenReturn(ISSUER);
    when(authentication.getPrincipal()).thenReturn(selfCareUser);

    // When
    boolean result = permissionEvaluator.hasPermission(authentication, null, PERMISSION);

    // Then
    assertFalse(result);
    verify(authentication).getPrincipal();
    verify(selfCareUser).getIssuer();
  }

//  @Test // TO BE Used after IAM release
//  void hasPermission_shouldThrowException_whenPermissionIsNull() {
//    // Given
//    FilterAuthorityDomain filterAuthorityDomain = new FilterAuthorityDomain(ISSUER);
//
//    // When & Then
//    assertThrows(IllegalArgumentException.class, () ->
//      permissionEvaluator.hasPermission(authentication, filterAuthorityDomain, null)
//    );
//  }

  @Test
  void hasPermissionWithTargetId_shouldReturnFalse() {
    // When
    boolean result = permissionEvaluator.hasPermission(authentication, 1L, "TARGET_TYPE", PERMISSION);

    // Then
    assertFalse(result);
    verifyNoInteractions(authentication);
  }

  @Test
  void hasPermission_shouldReturnFalse_whenIssuerIsNull() {
    // Given
    when(authentication.getPrincipal()).thenReturn(selfCareUser);
    when(selfCareUser.getIssuer()).thenReturn(null);
    FilterAuthorityDomain filterAuthorityDomain = new FilterAuthorityDomain(ISSUER);

    // When & Then
    assertThrows(NullPointerException.class, () ->
      permissionEvaluator.hasPermission(authentication, filterAuthorityDomain, PERMISSION)
    );
  }

  @Test
  void hasPermission_shouldReturnTrue_whenBothIssuersAreNull() {
    // Given
    when(authentication.getPrincipal()).thenReturn(selfCareUser);
    when(selfCareUser.getIssuer()).thenReturn(null);
    FilterAuthorityDomain filterAuthorityDomain = new FilterAuthorityDomain(null);

    // When & Then
    assertThrows(NullPointerException.class, () ->
      permissionEvaluator.hasPermission(authentication, filterAuthorityDomain, PERMISSION)
    );
  }
}
