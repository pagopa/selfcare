package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.Data;

import java.util.List;

@Data
public class BulkInstitutions {
    private List<BulkInstitution> found;
    private List<String> notFound;
}
