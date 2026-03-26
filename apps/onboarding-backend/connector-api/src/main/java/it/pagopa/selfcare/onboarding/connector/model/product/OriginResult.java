package it.pagopa.selfcare.onboarding.connector.model.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OriginResult {
    private List<OriginEntry> origins;
}

