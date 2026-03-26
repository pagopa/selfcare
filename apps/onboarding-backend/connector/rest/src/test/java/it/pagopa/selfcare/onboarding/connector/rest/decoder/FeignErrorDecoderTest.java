package it.pagopa.selfcare.onboarding.connector.rest.decoder;

import feign.Request;
import feign.Response;
import it.pagopa.selfcare.onboarding.connector.exceptions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static feign.Util.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class FeignErrorDecoderTest {

    FeignErrorDecoder feignDecoder = new FeignErrorDecoder();
    private String jsonError = """
            {
              "status": 400,
              "errors": [
                {
                  "code": "002-1003",
                  "detail": "Only CAdES signature form is admitted. Invalid signatures forms detected: PKCS7"
                }
              ]
            }
            """;

    private Map<String, Collection<String>> headers = new LinkedHashMap<>();

    @Test
    void testDecodeInvalidRequest() throws Throwable {
        //given
        Response response = Response.builder()
                .status(400)
                .reason("InvalidRequest")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertThrows(InvalidRequestException.class, executable);
    }

    @Test
    void testDecodeResourceConflict() throws Throwable {
        //given
        Response response = Response.builder()
                .status(409)
                .reason("ResourceConflict")
                .request(Request.create(Request.HttpMethod.POST, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertThrows(ResourceConflictException.class, executable);
    }

    @Test
    void testDecodeToResourceNotFound() throws Throwable {
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
    void testDecodeToServerError() throws Throwable {
        //given
        Response response = Response.builder()
                .status(500)
                .reason("ResourceNotFound")
                .request(Request.create(Request.HttpMethod.GET, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertThrows(InternalGatewayErrorException.class, executable);
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

    @Test
    void testDecodeCustomSignVerificationException() throws Throwable {
        //given
        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(Request.create(Request.HttpMethod.POST, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body(jsonError, UTF_8)
                .build();

        // when
        Executable executable = () -> {
            throw feignDecoder.decode("anyMethodKey", response);
        };

        // then
        Exception exception = assertThrows(CustomVerifyException.class, executable);
        assertEquals(400, ((CustomVerifyException) exception).getStatus());
        assertEquals(jsonError, ((CustomVerifyException) exception).getBody());
    }

}