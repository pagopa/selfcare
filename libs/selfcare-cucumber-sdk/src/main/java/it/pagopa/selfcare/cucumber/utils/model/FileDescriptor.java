package it.pagopa.selfcare.cucumber.utils.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileDescriptor {

    String filePathReference;
    String keyParamRequest;
    String mediaType;

}
