package it.pagopa.selfcare.user_group.connector.dao.auditing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        log.trace("getCurrentAuditor start");
        Optional<String> result = Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName);
        log.debug("getCurrentAuditor result = {}", result);
        log.trace("getCurrentAuditor end");
        return result;
    }

}