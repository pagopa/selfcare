package it.pagopa.selfcare.onboarding.crypto.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import it.pagopa.selfcare.onboarding.crypto.entity.SignRequest;
import org.apache.pdfbox.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class NamirialHttpClient {

    private static final String NAMIRIAL_BASE_URL = System.getenv("NAMIRIAL_BASE_URL");

    private static final String NAMIRIAL_SIGN_PADES_URL = NAMIRIAL_BASE_URL + "/SignEngineWeb/rest/service/signPAdES";

    public byte[] signDocument(SignRequest request) throws IOException {
        // Initialize HTTP Transport and Request Factory
        HttpTransport httpTransport = new NetHttpTransport();
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory();

        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";

        // Create the multipart content
        MultipartContent multipartContent = new MultipartContent()
                .setBoundary(boundary);
        ObjectMapper objectMapper = new ObjectMapper();


        String credentials = objectMapper.writeValueAsString(request.getCredentials());
        String preferences = objectMapper.writeValueAsString(request.getPreferences());


        multipartContent.addPart(
                new MultipartContent.Part(
                        new HttpHeaders().set("Content-Disposition", "form-data; name=\"file\"; filename=\""+ request.getFile().getName() +"\""),
                        new FileContent("application/pdf", request.getFile())
                )
        );



        multipartContent.addPart(
                new MultipartContent.Part(
                        new HttpHeaders().set("Content-Disposition", "form-data; name=\"credentials\""),
                        new ByteArrayContent("application/json", credentials.getBytes())
                )
        );


        multipartContent.addPart(
                new MultipartContent.Part(
                        new HttpHeaders().set("Content-Disposition", "form-data; name=\"preferences\""),
                        new ByteArrayContent("application/json", preferences.getBytes())
                )
        );

        // Build and execute the HTTP POST request
        HttpRequest httpRequest = requestFactory.buildPostRequest(
                new GenericUrl(NAMIRIAL_SIGN_PADES_URL), multipartContent);

        // Set any required headers
        httpRequest.getHeaders().setContentType("multipart/form-data; boundary=" + boundary);


        try {
            HttpResponse httpResponse = httpRequest.execute();
            InputStream is = httpResponse.getContent();
            return IOUtils.toByteArray(is);
        } catch (HttpResponseException e) {
            throw new IllegalStateException("Something gone wrong when invoking Namirial in order to calculate pkcs7 hash sign request", e);
        }
    }
}