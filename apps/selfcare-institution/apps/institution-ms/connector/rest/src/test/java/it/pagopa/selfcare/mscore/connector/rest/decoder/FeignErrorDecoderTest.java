package it.pagopa.selfcare.mscore.connector.rest.decoder;

import feign.Request;
import feign.Response;
import it.pagopa.selfcare.mscore.exception.BadGatewayException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(ints = {500,502,509})
    void testDecodeToBadGateway(int status) {
        //given
        Response response = Response.builder()
                .status(status)
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

    @ParameterizedTest
    @ValueSource(ints = {504,503,509})
    void testDecodeServiceUnvailable() {
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
