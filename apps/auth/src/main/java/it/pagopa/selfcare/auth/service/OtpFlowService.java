package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.entity.OtpFlow;

public interface OtpFlowService {

  Uni<OtpFlow> createNewOtpFlow(String userId, String otp);

  Uni<OtpFlow> findLastOtpFlowByUserId(String userId);
}
