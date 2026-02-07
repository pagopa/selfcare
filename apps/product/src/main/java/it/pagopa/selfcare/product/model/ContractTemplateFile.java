package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import java.io.File;
import lombok.*;

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
