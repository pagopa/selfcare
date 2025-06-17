package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.error.OtpStatus;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.commons.codec.digest.Md5Crypt;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public interface OtpFlowService {

  Uni<OtpFlow> createNewOtpFlow(String userId, String otp);

  Uni<OtpFlow> findLastOtpFlowByUserId(String userId);
}
