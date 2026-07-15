package it.pagopa.selfcare.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OnboardingUserDeleteInfo {
    String tokenId;
    String userId;
}
