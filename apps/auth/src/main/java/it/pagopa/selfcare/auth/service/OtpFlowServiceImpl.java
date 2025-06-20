package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.util.OtpUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OtpFlowServiceImpl implements OtpFlowService {

  @ConfigProperty(name = "otp.duration")
  Integer otpDuration;

  @Override
  public Uni<OtpFlow> createNewOtpFlow(String userId, String otp) {
    return Uni.createFrom()
        .item(OffsetDateTime.now())
        .map(
            now ->
                OtpFlow.builder()
                    .uuid(UUID.randomUUID().toString())
                    .userId(userId)
                    .attempts(0)
                    .otp(DigestUtils.md5Hex(otp))
                    .status(OtpStatus.PENDING)
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(otpDuration))
                    .build())
        .chain(otpFlow -> OtpFlow.persist(otpFlow).map(v -> otpFlow));
  }

  @Override
  public Uni<OtpFlow> findLastOtpFlowByUserId(String userId) {
    final String userIdField = OtpFlow.Fields.userId.name();
    final String createdAtField = OtpFlow.Fields.createdAt.name();
    return OtpFlow.find(new Document(userIdField, userId), new Document(createdAtField, -1))
        .firstResult();
  }
}
