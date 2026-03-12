package it.pagopa.selfcare.document.model;

import java.io.File;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FormItem {
    private File file;
    private String fileName;
}
