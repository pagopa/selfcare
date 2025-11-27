package it.pagopa.selfcare.product.controller.response;

import it.pagopa.selfcare.product.model.OriginEntry;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;


@ToString(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class ProductOriginResponse {
    private List<OriginEntry> origins;
}
