package it.pagopa.selfcare.user.integration_test.utils;

import io.restassured.response.ExtractableResponse;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@ApplicationScoped
@Getter
@Setter
public class SharedStepData {

    private String token;
    private String requestBody;
    private Map<String, String> pathParams;
    private Map<String, List<String>> queryParams;
    private ExtractableResponse<?> response;

    public void clear() {
        this.token = null;
        this.requestBody = null;
        this.pathParams = null;
        this.queryParams = null;
        this.response = null;
    }

}
