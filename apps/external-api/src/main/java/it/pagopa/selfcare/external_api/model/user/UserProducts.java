package it.pagopa.selfcare.external_api.model.user;

import lombok.Data;

import java.util.List;

@Data
public class UserProducts {
    private List<InstitutionProducts> bindings;
    private String id;
}
