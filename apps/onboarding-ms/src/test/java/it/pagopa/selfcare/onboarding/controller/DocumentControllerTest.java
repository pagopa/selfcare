package it.pagopa.selfcare.onboarding.controller;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
@TestHTTPEndpoint(DocumentController.class)
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class DocumentControllerTest {

    @InjectMock
    AzureBlobClient blobClient;

    @Test
    @TestSecurity(user = "userJwt")
    void getFilesFromPath_ByPath_OK() {
        // given
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        final String path = "/test/test";
        var builderPath = Base64.getEncoder().encodeToString(path.getBytes());
        List<String> result = new ArrayList<>();

        // when
        when(blobClient.getFiles(captor.capture())).thenReturn(result);

        given()
            .when()
            .contentType(ContentType.JSON)
            .pathParam("path", builderPath)
            .get("{path}")
            .then()
            .statusCode(200);

        // then
        verify(blobClient, times(1)).getFiles(anyString());
        assertEquals(captor.getValue(), path);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getFiles_OK() {
        // given
        List<String> result = new ArrayList<>();

        // when
        when(blobClient.getFiles()).thenReturn(result);

        given()
            .when()
            .contentType(ContentType.JSON)
            .get()
            .then()
            .statusCode(200);

        // then
        verify(blobClient, times(1)).getFiles();

    }

}
