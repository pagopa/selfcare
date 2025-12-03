package it.pagopa.selfcare.product.storage;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.ContractTemplateFile;

public interface ContractTemplateStorage {

    Uni<Void> upload(String productId, String contractTemplateId, ContractTemplateFile contractTemplateFile);

    Uni<ContractTemplateFile> download(String productId, String contractTemplateId);

}
