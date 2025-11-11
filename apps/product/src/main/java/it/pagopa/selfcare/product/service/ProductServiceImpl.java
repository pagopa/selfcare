package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ProductServiceImpl implements ProductService {


  @Override
  public Uni<String> ping() {
    return Uni.createFrom().item("OK");
  }
}
