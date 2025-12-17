package it.pagopa.selfcare.product.model.dto.request;

import lombok.*;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractTemplateUploadRequest {

    private String productId;

    private String name;

    private String version;

    private String description;

    private String createdBy;

    private FileUpload file;

}
