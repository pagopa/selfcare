package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.api.IamConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IamServiceImpl implements IamService {

    private final IamConnector iamConnector;

    @Override
    public boolean hasIamUserPermission(String permission, String userId, String institutionId, String productId) {
        log.trace("hasIamUserPermission start");
        boolean hasPermission = iamConnector.hasUserPermission(permission, userId, institutionId, productId);
        log.trace("hasIamUserPermission end");
        return hasPermission;
    }
}
