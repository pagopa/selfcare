package it.pagopa.selfcare.onboarding.crypto;



import com.sun.xml.ws.developer.JAXWSProperties;
import it.pagopa.selfcare.onboarding.crypto.config.ArubaInitializer;
import it.pagopa.selfcare.onboarding.crypto.config.ArubaSignConfig;
import it.pagopa.selfcare.onboarding.crypto.soap.aruba.sign.generated.client.*;
import it.pagopa.selfcare.onboarding.crypto.utils.CryptoUtils;
import it.pagopa.selfcare.onboarding.crypto.utils.SoapLoggingHandler;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ArubaSignServiceImpl implements ArubaSignService {

    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ArubaSignConfig config;
    private final SoapLoggingHandler soapLoggingHandler;

    private final ArubaSignServiceService arubaSignServiceService;

    public ArubaSignServiceImpl() {
        this.config = ArubaInitializer.initializeConfig();
        this.soapLoggingHandler = new SoapLoggingHandler();

        this.arubaSignServiceService = new ArubaSignServiceService(getClass().getClassLoader().getResource("docs/aruba/ArubaSignService.wsdl"));
    }

    private it.pagopa.selfcare.onboarding.crypto.soap.aruba.sign.generated.client.ArubaSignService getClient() {
        it.pagopa.selfcare.onboarding.crypto.soap.aruba.sign.generated.client.ArubaSignService client = arubaSignServiceService.getArubaSignServicePort();
        ((BindingProvider) client).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, config.getBaseUrl());

        if (config.getConnectTimeoutMs() > 0) {
            ((BindingProvider) client).getRequestContext().put(JAXWSProperties.CONNECT_TIMEOUT, config.getConnectTimeoutMs());
        }
        if (config.getRequestTimeoutMs() > 0) {
            ((BindingProvider) client).getRequestContext().put(JAXWSProperties.REQUEST_TIMEOUT, config.getRequestTimeoutMs());
        }

        @SuppressWarnings("rawtypes")
        List<Handler> handlerChain = ((BindingProvider) client).getBinding().getHandlerChain();
        handlerChain.add(soapLoggingHandler);
        ((BindingProvider) client).getBinding().setHandlerChain(handlerChain);

        return client;
    }

//region hashSign request
    @Override
    public byte[] hashSign(InputStream is) {
        it.pagopa.selfcare.onboarding.crypto.soap.aruba.sign.generated.client.ArubaSignService client = getClient();
        SignHashRequest request = buildSignHashRequest(is);
        SignHashReturn result = client.signhash(request);
        if ("OK".equals(result.getStatus())) {
            return result.getSignature();
        } else {
            throw new IllegalStateException(String.format("Something gone wrong while signing input file: returnCode: %s; description:%s", result.getReturnCode(), result.getDescription()));
        }
    }

    private SignHashRequest buildSignHashRequest(InputStream is) {
        SignHashRequest request = new SignHashRequest();

        request.setCertID("AS0");
        request.setIdentity(config.getAuth());

        request.setHash(CryptoUtils.getDigest(is));
        request.setHashtype("SHA256");

        request.setRequirecert(false);

        return request;
    }
//endregion

//region pkcs7Signhash request
    @Override
    public byte[] pkcs7Signhash(InputStream is) {
        it.pagopa.selfcare.onboarding.crypto.soap.aruba.sign.generated.client.ArubaSignService client = getClient();
        SignRequestV2 request = buildSignRequest(is);
        try {
            SignReturnV2 result = client.pkcs7Signhash(request, false, false);
            if ("OK".equals(result.getStatus())) {
                return result.getBinaryoutput();
            } else {
                throw new IllegalStateException(String.format("Something gone wrong while performing pkcs7 hash sign request: returnCode: %s; description:%s", result.getReturnCode(), result.getDescription()));
            }
        } catch (TypeOfTransportNotImplemented_Exception e) {
            throw new IllegalStateException("Something gone wrong when invoking Aruba in order to calculate pkcs7 hash sign request", e);
        }
    }

    private SignRequestV2 buildSignRequest(InputStream is) {
        SignRequestV2 request = new SignRequestV2();

        request.setCertID("AS0");
        request.setProfile("NULL");
        request.setIdentity(config.getAuth());

        request.setTransport(TypeTransport.BYNARYNET);
        request.setBinaryinput(CryptoUtils.getDigest(is));

        request.setRequiredmark(false);
        request.setSigningTime(df.format(LocalDateTime.now()));

        return request;
    }
//endregion

}
