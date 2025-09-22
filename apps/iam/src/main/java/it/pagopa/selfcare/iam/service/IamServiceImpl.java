package it.pagopa.selfcare.iam.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class IamServiceImpl implements IamService {

  @Override
  public Uni<String> ping() {
    return Uni.createFrom().item("OK");
  }
}
