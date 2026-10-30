package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.model.document.DocumentStatusResource;
import it.pagopa.selfcare.dashboard.model.document.DocumentsResource;

import java.util.List;

public interface DocumentService {

    DocumentStatusResource getDocumentStatus(String institutionId, List<String> productIds);

    DocumentsResource getDocuments(String institutionId, String productId);

}
