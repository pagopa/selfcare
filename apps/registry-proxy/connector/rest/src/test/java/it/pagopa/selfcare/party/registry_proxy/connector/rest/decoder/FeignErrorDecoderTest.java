package it.pagopa.selfcare.party.registry_proxy.connector.rest.decoder;

import feign.Request;
import feign.Response;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.BadGatewayException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static feign.Util.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeignErrorDecoderTest {
    FeignErrorDecoder feignDecoder = new FeignErrorDecoder();

    private final Map<String, Collection<String>> headers = new LinkedHashMap<>();

    @Test
    void testDecodeToResourceNotFound() {
        //given
        Response response = Response.builder()
                .status(404)
                .reason("ResourceNotFound")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
    }

    @Test
    void testDecodeToBadGateway() {
        //given
        Response response = Response.builder()
                .status(502)
                .reason("Bad Gateway")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertThrows(BadGatewayException.class, executable);
    }

    @Test
    void testDecodeToBadGateway2() {
        //given
        Response response = Response.builder()
                .status(500)
                .reason("Bad Gateway")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertThrows(BadGatewayException.class, executable);
    }

    @Test
    void testDecodeToBadGateway3() {
        //given
        Response response = Response.builder()
                .status(509)
                .reason("Bad Gateway")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertThrows(BadGatewayException.class, executable);
    }

    @Test
    void testDecodeServiceUnvailable1() {
        //given
        Response response = Response.builder()
                .status(504)
                .reason("Service Unvailable")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
    }


    @Test
    void testDecodeServiceUnvailable2() {
        //given
        Response response = Response.builder()
                .status(503)
                .reason("Service Unvailable")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
    }


    @Test
    void testDecodeDefault() {
        //given
        Response response = Response.builder()
                .status(200)
                .reason("OK")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertDoesNotThrow(executable);
    }
}
