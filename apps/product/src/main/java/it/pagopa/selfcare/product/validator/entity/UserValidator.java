package it.pagopa.selfcare.product.validator.entity;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class UserValidator {
  /*
  public void validateRoles(Map<String, UserRolePermission> roleMappings) {
      if (roleMappings != null) {
          roleMappings.keySet().forEach(role -> {
              try {
                  UserRole.valueOf(role);
                  log.debug("Validated role: {}", role);
              } catch (IllegalArgumentException e) {
                  throw new InvalidRoleException("Invalid role: " + role);
              }
          });
      }
  }
  */
}
