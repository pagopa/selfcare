package it.pagopa.selfcare.party.registry_proxy.web.security;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.Serializable;

@Component
@Slf4j
public class SelfCarePermissionEvaluator implements PermissionEvaluator {

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
    log.info("start check Permission");
    log.debug(LogUtils.CONFIDENTIAL_MARKER, "hasPermission authentication = {}, targetDomainObject = {}, permission = {}", authentication, targetDomainObject, permission);
    Assert.notNull(permission, "A permission type is required");
    boolean result = false;
    String issuer = ((SelfCareUser) authentication.getPrincipal()).getIssuer();
    if (targetDomainObject instanceof FilterAuthorityDomain filterAuthorityDomain) {
      result = issuer.equals(filterAuthorityDomain.getIssuer());
      log.debug("check Permission result = {}", result);
      log.trace("check Permission end");
    }
    return result;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
    return false;
  }
}
