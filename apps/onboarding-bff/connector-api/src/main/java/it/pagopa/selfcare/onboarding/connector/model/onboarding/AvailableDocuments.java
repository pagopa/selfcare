package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import lombok.Data;

import java.util.List;

@Data
public class AvailableDocuments {

    private List<String> attachments;
    private String contractFilename;
}

