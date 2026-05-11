package it.pagopa.selfcare.dashboard.model.delegation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DelegationResource {

        private String id;
        private String institutionId;

        private String institutionName;
        private String institutionRootName;
        private String brokerName;
        private DelegationType type;
        private String brokerId;
        private String productId;

}
