package it.pagopa.selfcare.commons.web.validator;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


public abstract class PointcutControllerResponseValidatorBaseTest {

    protected abstract ControllerResponseValidator getValidatorSpy();

    protected abstract void invokeNotVoidMethod();

    protected abstract void invokeVoidMethod();

    @Test
    void controllersPointcut_returnNotVoid() {
        // given
        // when
        invokeNotVoidMethod();
        // then
        Mockito.verify(getValidatorSpy(), Mockito.times(1))
                .validateResponse(Mockito.any(), Mockito.any());
        Mockito.verifyNoMoreInteractions(getValidatorSpy());
    }

    @Test
    void controllersPointcut_returnVoid() {
        // given
        // when
        invokeVoidMethod();
        // then
        Mockito.verify(getValidatorSpy(), Mockito.times(1))
                .validateResponse(Mockito.any(), Mockito.any());
        Mockito.verifyNoMoreInteractions(getValidatorSpy());
    }

    @Test
    void controllersPointcut() {//sonar fix
        getValidatorSpy().controllersPointcut();
    }
}