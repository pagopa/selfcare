package it.pagopa.selfcare.onboarding.repository;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.context.TenantDataIsolation;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.utils.QueryUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.bson.Document;

@ApplicationScoped
public class OnboardingRepository implements PanacheMongoRepositoryBase<Onboarding, String> {

    @Inject
    TenantDataIsolation tenantDataIsolation;

    @Override
    public void persist(Onboarding onboarding) {
        assignTenant(onboarding);
        PanacheMongoRepositoryBase.super.persist(onboarding);
    }

    @Override
    public void persistOrUpdate(Onboarding onboarding) {
        assignTenant(onboarding);
        PanacheMongoRepositoryBase.super.persistOrUpdate(onboarding);
    }

    @Override
    public void update(Onboarding onboarding) {
        assignTenant(onboarding);
        PanacheMongoRepositoryBase.super.update(onboarding);
    }

    private void assignTenant(Onboarding onboarding) {
        onboarding.setTenantId(tenantDataIsolation.tenantForWrite(onboarding.getTenantId()));
    }

    public List<Onboarding> findByFilters(String taxCode, String subunitCode, String origin, String originId, String productId) {
        final Map<String, String> queryParameter =
                QueryUtils.createMapForInstitutionOnboardingsQueryParameter(
                        taxCode, subunitCode, origin, originId, OnboardingStatus.COMPLETED, productId);
        Document query = QueryUtils.buildQuery(queryParameter);
        return find(query).list();
    }

    public List<Onboarding> findByOnboardingUsers(String institutionId, String productId) {
        final Map<String, String> queryParameter =
                QueryUtils.createMapForOnboardingUsers(institutionId, productId);
        Document query = QueryUtils.buildQuery(queryParameter);
        return find(query).list();
    }
}
