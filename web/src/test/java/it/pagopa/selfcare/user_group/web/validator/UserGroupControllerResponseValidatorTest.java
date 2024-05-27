package it.pagopa.selfcare.user_group.web.validator;

import it.pagopa.selfcare.commons.web.validator.ControllerResponseValidator;
import it.pagopa.selfcare.commons.web.validator.PointcutControllerResponseValidatorBaseTest;
import it.pagopa.selfcare.user_group.web.controller.DummyController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootTest(classes = {
        ValidationAutoConfiguration.class,
        DummyController.class,
        UserGroupControllerResponseValidator.class
})
@EnableAspectJAutoProxy
class UserGroupControllerResponseValidatorTest extends PointcutControllerResponseValidatorBaseTest {
    @Autowired
    private DummyController controller;

    @SpyBean
    private UserGroupControllerResponseValidator validator;

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