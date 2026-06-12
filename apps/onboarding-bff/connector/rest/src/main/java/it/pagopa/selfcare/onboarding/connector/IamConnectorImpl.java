package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.iam.generated.openapi.v1.dto.PermissionResponse;
import it.pagopa.selfcare.onboarding.connector.api.IamConnector;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsIamRestClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IamConnectorImpl implements IamConnector {

    private final MsIamRestClient msIamRestClient;

    @Override
    public boolean hasUserPermission(String permission, String userId, String institutionId, String productId) {
        return Optional.ofNullable(msIamRestClient._hasIAMUserPermission(permission, userId, institutionId, productId).getBody())
                .map(PermissionResponse::getHasPermission)
                .orElse(false);
    }
}
