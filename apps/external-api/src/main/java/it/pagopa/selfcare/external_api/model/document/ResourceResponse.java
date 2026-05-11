package it.pagopa.selfcare.external_api.model.document;

import lombok.Data;

@Data
public class ResourceResponse {
    private byte[] data;
    private String fileName;
    private String mimetype;
}
