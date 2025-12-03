package it.pagopa.selfcare.product.model.dto.request;

import it.pagopa.selfcare.product.validator.AllowedFileTypes;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Getter
@Setter
public class ContractTemplateUploadRequest {

    @QueryParam("productId")
    @NotNull
    private String productId;

    @RestForm("file")
    @NotNull
    @AllowedFileTypes(value = {AllowedFileTypes.HTML}, message = "Only static HTML files are allowed")
    private FileUpload file;

    @RestForm("name")
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "The field can only contain letters, numbers, and spaces")
    private String name;

    @RestForm("version")
    @NotNull
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "The format must be X.X.X where X is a number")
    private String version;

    @RestForm("description")
    private String description;

    @RestForm("createdBy")
    private String createdBy;

}
