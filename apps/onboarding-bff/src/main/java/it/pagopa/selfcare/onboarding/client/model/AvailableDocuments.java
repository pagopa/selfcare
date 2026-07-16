package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

import java.util.List;

@Data
public class AvailableDocuments {
    private List<String> attachments;
    private String contractFilename;
}
