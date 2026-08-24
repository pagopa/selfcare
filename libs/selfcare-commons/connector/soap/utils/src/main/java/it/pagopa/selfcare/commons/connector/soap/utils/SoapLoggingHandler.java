package it.pagopa.selfcare.commons.connector.soap.utils;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@Service
@Slf4j
public class SoapLoggingHandler implements SOAPHandler<SOAPMessageContext> {

    @Override
    public void close(MessageContext msg) {
        // Do Nothing
    }

    @Override
    public boolean handleFault(SOAPMessageContext msg) {
        SOAPMessage message = msg.getMessage();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            log.info("Obtained a fault message: {}", outputStream);
        } catch (SOAPException | IOException e) {
            log.error("Something gone wrong while tracing soap fault message");
        }
        return true;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext msg) {
        if(log.isDebugEnabled()) {
            SOAPMessage message = msg.getMessage();
            boolean isOutboundMessage = (Boolean) msg.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            String msgType = isOutboundMessage
                    ? "OUTBOUND MESSAGE"
                    : "INBOUND MESSAGE";
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                message.writeTo(outputStream);
                log.debug("Obtained a {} message: {}", msgType, outputStream);
            } catch (SOAPException | IOException e) {
                log.error(String.format("Something gone wrong while tracing soap %s", msgType));
            }
        }
        return true;
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

}