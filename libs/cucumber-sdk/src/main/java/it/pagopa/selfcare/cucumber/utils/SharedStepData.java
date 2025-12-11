package it.pagopa.selfcare.cucumber.utils;

import io.restassured.response.ExtractableResponse;
import it.pagopa.selfcare.cucumber.utils.model.FileDescriptor;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@ApplicationScoped
@Component
public class SharedStepData {

    private String token;
    private String requestBody;
    private Map<String, String> pathParams;
    private Map<String, List<String>> queryParams;
    private Map<String, String> formData;
    private FileDescriptor fileUpload;
    private ExtractableResponse<?> response;
    private Map<FileDescriptor, Object> contentMultiPart;

    public void clear() {
        this.token = null;
        this.requestBody = null;
        this.pathParams = null;
        this.queryParams = null;
        this.formData = null;
        this.fileUpload = null;
        this.response = null;
        this.contentMultiPart = null;
    }

}
