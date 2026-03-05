package it.pagopa.selfcare.onboarding.crypto.utils;

import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

class SoapLoggingHandlerTest {

    private final SoapLoggingHandler soapLoggingHandler = new SoapLoggingHandler();

    @Test
    void testInbound(){
        SOAPMessageContext messageMock = getSoapMessageMock(false);

        Assertions.assertTrue(soapLoggingHandler.handleMessage(messageMock));
    }

    @Test
    void testOutbound(){
        SOAPMessageContext messageMock = getSoapMessageMock(true);

        Assertions.assertTrue(soapLoggingHandler.handleMessage(messageMock));
    }

    private SOAPMessageContext getSoapMessageMock(boolean t) {
        SOAPMessageContext messageMock = Mockito.mock(SOAPMessageContext.class);
        Mockito.when(messageMock.getMessage()).thenReturn(Mockito.mock(SOAPMessage.class));
        Mockito.when(messageMock.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)).thenReturn(t);
        return messageMock;
    }

    @Test
    void testFault(){
        SOAPMessageContext messageMock = Mockito.mock(SOAPMessageContext.class);
        Mockito.when(messageMock.getMessage()).thenReturn(Mockito.mock(SOAPMessage.class));

        Assertions.assertTrue(soapLoggingHandler.handleFault(messageMock));
    }

    @Test
    void testGetHeaders(){
        Assertions.assertEquals(
                Collections.emptySet(),
                soapLoggingHandler.getHeaders()
        );
    }
}
