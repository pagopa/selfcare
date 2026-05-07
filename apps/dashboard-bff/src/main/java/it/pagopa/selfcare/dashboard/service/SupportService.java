package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.model.support.SupportRequest;
import it.pagopa.selfcare.dashboard.model.support.SupportResponse;

public interface SupportService {
    SupportResponse sendRequest(SupportRequest supportRequest);
}
