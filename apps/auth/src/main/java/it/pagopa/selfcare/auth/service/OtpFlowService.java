package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.model.otp.OtpInfo;

import java.util.Optional;

public interface OtpFlowService {

  Uni<Optional<OtpInfo>> handleOtpFlow(UserClaims userClaims);

  Uni<OtpFlow> createNewOtpFlow(String userId, String otp);

  Uni<OtpFlow> findLastOtpFlowByUserId(String userId);
}
