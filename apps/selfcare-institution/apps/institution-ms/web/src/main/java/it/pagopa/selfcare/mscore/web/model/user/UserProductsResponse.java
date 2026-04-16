package it.pagopa.selfcare.mscore.web.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProductsResponse {

    private String id;
    private List<InstitutionProducts> bindings;
}
