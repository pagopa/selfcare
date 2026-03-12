package it.pagopa.selfcare.onboarding.model;

import lombok.Builder;
import lombok.Getter;

import java.io.File;

@Builder
@Getter
public class FormItem {
    private File file;
    private String fileName;
}
