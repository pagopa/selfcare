package it.pagopa.selfcare.commons.web.validator;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import jakarta.validation.Validation;
import java.util.List;

class ControllerResponseValidatorTest {

    private ControllerResponseValidator controllerResponseValidator =
            new DummyControllerResponseValidator(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void validateResponse_nullResponse() {
        // given
        JoinPoint joinPoint = null;
        Object result = null;
        // when
        Executable executable = () -> controllerResponseValidator.validateResponse(joinPoint, result);
        // then
        Assertions.assertDoesNotThrow(executable);
    }


    @Test
    void validateResponse_invalidResponse() {
        // given
        JoinPoint joinPoint = null;
        DummyResource resource = new DummyResource(null);
        // when
        Executable executable = () -> controllerResponseValidator.validateResponse(joinPoint, resource);
        // then
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, executable);
        Assertions.assertEquals("{dummy=[NotNull constraint violation]}", exception.getMessage());
    }


    @Test
    void validateResponse_invalidResponseCollection() {
        // given
        JoinPoint joinPoint = null;
        List<DummyResource> resources = List.of(new DummyResource(null), new DummyResource(null));
        // when
        Executable executable = () -> controllerResponseValidator.validateResponse(joinPoint, resources);
        // then
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, executable);
        Assertions.assertEquals("{dummy=[NotNull constraint violation]}", exception.getMessage());
    }


    @Test
    void validateResponse_validResponse() {
        // given
        JoinPoint joinPoint = null;
        DummyResource resource = new DummyResource("dummy");
        // when
        Executable executable = () -> controllerResponseValidator.validateResponse(joinPoint, resource);
        // then
        Assertions.assertDoesNotThrow(executable);
    }


    @Test
    void validateResponse_validResponseCollection() {
        // given
        JoinPoint joinPoint = null;
        List<DummyResource> resources = List.of(new DummyResource("dummy"), new DummyResource("dummy"));
        // when
        Executable executable = () -> controllerResponseValidator.validateResponse(joinPoint, resources);
        // then
        Assertions.assertDoesNotThrow(executable);
    }

}
