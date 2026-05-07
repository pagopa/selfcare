package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.dashboard.client.IamRestClient;
import it.pagopa.selfcare.dashboard.client.UserApiRestClient;
import it.pagopa.selfcare.dashboard.client.UserGroupRestClient;
import it.pagopa.selfcare.dashboard.security.SelfCarePermissionEvaluatorV2;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

    private final UserGroupRestClient userGroupRestClient;
    private final UserApiRestClient userApiRestClient;
    private final IamRestClient iamRestClient;


    public MethodSecurityConfig(UserGroupRestClient userGroupRestClient, UserApiRestClient userApiRestClient, IamRestClient iamRestClient) {
        this.userGroupRestClient = userGroupRestClient;
        this.userApiRestClient = userApiRestClient;
        this.iamRestClient = iamRestClient;
    }

    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(new SelfCarePermissionEvaluatorV2(userGroupRestClient, userApiRestClient, iamRestClient));
        return expressionHandler;
    }

}
