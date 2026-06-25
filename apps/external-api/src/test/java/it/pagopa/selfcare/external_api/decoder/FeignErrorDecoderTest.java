package it.pagopa.selfcare.external_api.decoder;

import feign.Request;
import feign.Response;
import it.pagopa.selfcare.external_api.client.decoder.FeignErrorDecoder;
import it.pagopa.selfcare.external_api.exception.InstitutionDoesNotExistException;
import it.pagopa.selfcare.external_api.exception.InvalidRequestException;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static feign.Util.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeignErrorDecoderTest {
    FeignErrorDecoder feignDecoder = new FeignErrorDecoder();

    private Map<String, Collection<String>> headers = new LinkedHashMap<>();

    @Test
    void testDecodeToInvalidRequest() throws Throwable {
        //given
        Response response = Response.builder()
                .status(400)
                .reason("Invalid Request")
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
    void testDecodeToInstitutionDoesNotExistException() throws Throwable {
        //given
        Response response = Response.builder()
                .status(400)
                .reason("BadRequest")
                .request(Request.create(Request.HttpMethod.HEAD, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertThrows(InstitutionDoesNotExistException.class, executable);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "Unauthorized,401",
            "OK,200"
    })
    void testDecodeToInstitutionDoesNotExistException_HttpMethodNotHead(String reason, Integer code) {
        //given
        Response response = Response.builder()
                .status(code)
                .reason(reason)
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
    void testDecodeToInstitutionDoesNotExistException_HttpStatusNot400() throws Throwable {
        //given
        Response response = Response.builder()
                .status(401)
                .reason("Unauthorized")
                .request(Request.create(Request.HttpMethod.HEAD, "/api", Collections.emptyMap(), null, UTF_8))
                .headers(headers)
                .body("hello world", UTF_8)
                .build();
        //when
        Executable executable = () -> feignDecoder.decode("", response);
        //then
        assertDoesNotThrow(executable);
    }

}
