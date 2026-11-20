package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.model.document.DocumentStatusResource;
import it.pagopa.selfcare.dashboard.model.document.DocumentType;
import it.pagopa.selfcare.dashboard.model.document.DocumentsResource;
import org.springframework.core.io.Resource;

import java.util.List;

public interface DocumentService {

    DocumentStatusResource getDocumentStatus(String institutionId, List<String> productIds);

    DocumentsResource getDocumentsList(String institutionId, String productId);

    Resource getDocument(String institutionId, String productId, String name, DocumentType type);

}
