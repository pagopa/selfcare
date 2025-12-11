package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import lombok.*;

import java.io.File;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplateFile {

    private File file;

    private byte[] data;

    private ContractTemplateFileType type;

}
