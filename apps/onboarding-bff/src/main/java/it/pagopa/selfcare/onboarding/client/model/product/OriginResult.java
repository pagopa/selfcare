package it.pagopa.selfcare.onboarding.client.model.product;

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

