package it.pagopa.selfcare.onboarding.dto;

import lombok.Data;

@Data
public class FileMailData {
    byte[] data;
    String name;
    String contentType;
}
