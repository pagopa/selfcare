package it.pagopa.selfcare.onboarding.entity;

import com.microsoft.azure.functions.HttpStatus;

public record ResponseWrapper(HttpStatus status, String body) {
}
