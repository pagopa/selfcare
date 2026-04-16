package it.pagopa.selfcare.mscore.model.onboarding;

import lombok.Data;

@Data
public class ResourceResponse {
    private byte[] data;
    private String fileName;
    private String mimetype;
}
