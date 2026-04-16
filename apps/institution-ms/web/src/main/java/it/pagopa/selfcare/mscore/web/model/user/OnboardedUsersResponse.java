package it.pagopa.selfcare.mscore.web.model.user;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OnboardedUsersResponse {
    private List<UserProductsResponse> users;
}
