package it.pagopa.selfcare.product.model.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractTemplateResponseList {

    private List<ContractTemplateResponse> contractTemplateResponses;

}
