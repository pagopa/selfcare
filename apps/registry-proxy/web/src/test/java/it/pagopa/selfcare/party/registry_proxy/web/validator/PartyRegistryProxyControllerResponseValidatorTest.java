package it.pagopa.selfcare.party.registry_proxy.web.validator;

import it.pagopa.selfcare.commons.web.validator.ControllerResponseValidator;
import it.pagopa.selfcare.commons.web.validator.PointcutControllerResponseValidatorBaseTest;
import it.pagopa.selfcare.party.registry_proxy.web.controller.DummyController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootTest(classes = {
        ValidationAutoConfiguration.class,
        DummyController.class,
        PartyRegistryProxyControllerResponseValidator.class
})
@EnableAspectJAutoProxy
class PartyRegistryProxyControllerResponseValidatorTest extends PointcutControllerResponseValidatorBaseTest {
    @Autowired
    private DummyController controller;

    @SpyBean
    private PartyRegistryProxyControllerResponseValidator validator;

    @Override
    protected ControllerResponseValidator getValidatorSpy() {
        return validator;
    }

    @Override
    protected void invokeNotVoidMethod() {
        controller.notVoidMethod();
    }

    @Override
    protected void invokeVoidMethod() {
        controller.voidMethod();
    }
}