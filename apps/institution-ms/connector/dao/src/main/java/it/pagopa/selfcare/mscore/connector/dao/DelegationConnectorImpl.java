package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.api.DelegationConnector;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationInstitutionEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.DelegationPageEntity;
import it.pagopa.selfcare.mscore.connector.dao.model.mapper.DelegationEntityMapper;
import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.constant.Order;
import it.pagopa.selfcare.mscore.model.delegation.*;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DelegationConnectorImpl implements DelegationConnector {

    public static final String COLLECTION_INSTITUTION = "Institution";
    public static final String COLLECTION_DELEGATIONS = "Delegations";

    private final DelegationRepository repository;
    private final DelegationEntityMapper delegationMapper;
    private final MongoTemplate mongoTemplate;

    public DelegationConnectorImpl(DelegationRepository repository,
                                   DelegationEntityMapper delegationMapper,
                                   MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.delegationMapper = delegationMapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Delegation save(Delegation delegation) {
        final DelegationEntity entity = delegationMapper.convertToDelegationEntity(delegation);
        return delegationMapper.convertToDelegation(repository.save(entity));
    }

    @Override
    public boolean checkIfExistsWithStatus(Delegation delegation, DelegationState status) {
        Optional<DelegationEntity> opt = repository.findByFromAndToAndProductIdAndTypeAndStatus(
                delegation.getFrom(),
                delegation.getTo(),
                delegation.getProductId(),
                delegation.getType(),
                status
        );
        return opt.isPresent();
    }

    private List<Criteria> getCriterias(String from, String to, String productId, String search, String taxCode) {
        List<Criteria> criterias = new ArrayList<>();

        criterias.add(Criteria.where(DelegationEntity.Fields.status.name()).is(DelegationState.ACTIVE.name()));

        if (Objects.nonNull(from)) {
            criterias.add(Criteria.where(DelegationEntity.Fields.from.name()).is(from));
            if(Objects.nonNull(taxCode)) {
                criterias.add(Criteria.where(DelegationEntity.Fields.toTaxCode.name()).is(taxCode));
            }
        }
        if (Objects.nonNull(to)) {
            criterias.add(Criteria.where(DelegationEntity.Fields.to.name()).is(to));
            if(Objects.nonNull(taxCode)) {
                criterias.add(Criteria.where(DelegationEntity.Fields.fromTaxCode.name()).is(taxCode));
            }
        }
        if (Objects.nonNull(productId)) {
            criterias.add(Criteria.where(DelegationEntity.Fields.productId.name()).is(productId));
        }
        if (Objects.nonNull(search)) {
            criterias.add(Criteria.where(DelegationEntity.Fields.institutionFromName.name()).regex("(?i)" + Pattern.quote(search)));
        }
        return criterias;
    }

    private List<Criteria> getCriterias(String from, String to, String productId, DelegationType type) {
        final List<Criteria> criterias = getCriterias(from, to, productId, null, null);
        Optional.ofNullable(type).ifPresent(t -> criterias.add(Criteria.where(DelegationEntity.Fields.type.name()).is(t)));
        return criterias;
    }

    @Override
    public List<Delegation> find(String from, String to, String productId, String search, String taxCode, Order order, Integer page, Integer size) {
        Criteria criteria = new Criteria();
        Pageable pageable = PageRequest.of(page, size);
        List<Criteria> criterias = getCriterias(from, to, productId, search, taxCode);

        Sort.Direction sortDirection = order.equals(Order.ASC) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Query query = Query.query(criteria.andOperator(criterias));

        if (!order.equals(Order.NONE)) {
            query = query.with(Sort.by(sortDirection, DelegationEntity.Fields.institutionFromName.name()));
        }

        return repository.find(query, pageable, DelegationEntity.class)
                .stream()
                .map(delegationMapper::convertToDelegation)
                .toList();
    }

    @Override
    public DelegationWithPagination findFromDate(OffsetDateTime fromDate, Long page, Long size) {
        final long effectivePage = page != null && page > 0 ? page : 0;
        final long effectiveSize = size != null && size > 0 ? size : 100;

        final MatchOperation matchOperation = Aggregation.match(Criteria.where(DelegationEntity.Fields.createdAt.name()).gte(fromDate));

        final SortOperation sortOperation = Aggregation.sort(Sort.Direction.ASC, "createdAt");

        final FacetOperation facetOperation = Aggregation.facet(Aggregation.count().as("totalElements")).as("pageInfo")
                .and(Aggregation.skip(effectivePage * effectiveSize), Aggregation.limit(effectiveSize)).as("delegations");

        final Aggregation aggregation = Aggregation.newAggregation(matchOperation, sortOperation, facetOperation);

        final DelegationPageEntity delegationPageEntity = mongoTemplate.aggregate(aggregation, COLLECTION_DELEGATIONS, DelegationPageEntity.class)
                .getMappedResults().stream().findFirst().orElseThrow();

        final List<Delegation> delegations = delegationPageEntity.getDelegations().stream().map(delegationMapper::convertToDelegation).toList();
        final long totalElements = delegationPageEntity.getPageInfo().stream().findFirst().map(DelegationPageEntity.PageInfo::getTotalElements).orElse(0L);
        final PageInfo pageInfo = new PageInfo(effectiveSize, effectivePage, totalElements, (long) Math.ceil((double) totalElements / effectiveSize));
        return new DelegationWithPagination(delegations, pageInfo);
    }

    @Override
    public DelegationWithPagination findAndCount(GetDelegationParameters delegationParameters) {

        List<Delegation> delegations = find(delegationParameters.getFrom(), delegationParameters.getTo(), delegationParameters.getProductId(),
                delegationParameters.getSearch(), delegationParameters.getTaxCode(), delegationParameters.getOrder(),
                delegationParameters.getPage(), delegationParameters.getSize());

        Query query = Query.query(new Criteria().andOperator(getCriterias(delegationParameters.getFrom(), delegationParameters.getTo(),
                delegationParameters.getProductId(), delegationParameters.getSearch(), delegationParameters.getTaxCode())));

        long count = mongoTemplate.count(query, DelegationEntity.class);

        Pageable pageable = PageRequest.of(delegationParameters.getPage(), delegationParameters.getSize());
        Page<Delegation> result = PageableExecutionUtils.getPage(delegations, pageable, () -> count);

        PageInfo pageInfo = new PageInfo(result.getSize(), result.getNumber(), result.getTotalElements(), result.getTotalPages());
        return new DelegationWithPagination(delegations, pageInfo);
    }

    @Override
    public Delegation findByIdAndModifyStatus(String delegationId, DelegationState status) {
        Query query = Query.query(Criteria.where(DelegationEntity.Fields.id.name()).is(delegationId));
        Update update = new Update();
        update.set(DelegationEntity.Fields.updatedAt.name(), OffsetDateTime.now());
        update.set(DelegationEntity.Fields.status.name(), status);

        if(Objects.equals(status, DelegationState.DELETED)){
            update.set(DelegationEntity.Fields.closedAt.name(), OffsetDateTime.now());
        } else {
            update.set(DelegationEntity.Fields.closedAt.name(), null);
        }

        FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().upsert(false);
        return delegationMapper.convertToDelegation(repository.findAndModify(query, update, findAndModifyOptions, DelegationEntity.class));
    }

    @Override
    public boolean checkIfDelegationsAreActive(String institutionId) {
        List<DelegationEntity> opt = repository.findByToAndStatus(institutionId, DelegationState.ACTIVE).orElse(Collections.emptyList());
        return !opt.isEmpty();
    }

    @Override
    public Delegation findAndActivate(String from, String to, String productId, Boolean isTest) {
        Query query = Query.query(Criteria.where(DelegationEntity.Fields.from.name()).is(from).and(DelegationEntity.Fields.to.name()).is(to).and(DelegationEntity.Fields.productId.name()).is(productId));
        Update update = new Update();
        update.set(DelegationEntity.Fields.updatedAt.name(), OffsetDateTime.now());
        update.set(DelegationEntity.Fields.closedAt.name(), null);
        update.set(DelegationEntity.Fields.status.name(), DelegationState.ACTIVE);
        if(Boolean.TRUE.equals(isTest)){
            update.set(DelegationEntity.Fields.isTest.name(), true);
        }
        FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().upsert(false).returnNew(true);
        return delegationMapper.convertToDelegation(repository.findAndModify(query, update, findAndModifyOptions, DelegationEntity.class));
    }

    @Override
    public void updateDelegation(Institution institutionUpdate) {

        // If institution own some delegations, we also update "to" reference
        // isDelegation is true if institution own some delegations
        if (institutionUpdate.isDelegation()) {
            Update updateFrom = new Update();
            Query queryFrom = Query.query(Criteria.where(DelegationEntity.Fields.to.name()).is(institutionUpdate.getId()));
            updateFrom.set(DelegationEntity.Fields.institutionToName.name(), institutionUpdate.getDescription());
            repository.updateMulti(queryFrom, updateFrom, DelegationEntity.class);
        }

        Update updateTo = new Update();
        Query queryTo = Query.query(Criteria.where(DelegationEntity.Fields.from.name()).is(institutionUpdate.getId()));
        updateTo.set(DelegationEntity.Fields.institutionFromName.name(), institutionUpdate.getDescription());
        if (Objects.nonNull(institutionUpdate.getParentDescription())) {
            updateTo.set(DelegationEntity.Fields.institutionFromRootName.name(), institutionUpdate.getParentDescription());
        }
        updateTo.set(DelegationEntity.Fields.updatedAt.name(), OffsetDateTime.now());
        repository.updateMulti(queryTo, updateTo, DelegationEntity.class);

    }

    @Override
    public List<DelegationInstitution> findDelegators(String institutionId, String productId, DelegationType type, Long cursor, int size) {
        final List<Criteria> matchCriterias = getCriterias(null, institutionId, productId, type);
        Optional.ofNullable(cursor).ifPresent(c -> matchCriterias.add(Criteria.where("createdAt")
                .gt(OffsetDateTime.ofInstant(Instant.ofEpochMilli(c), ZoneId.systemDefault()))));

        // Filter delegations
        final MatchOperation matchOperation = Aggregation.match(new Criteria().andOperator(matchCriterias));

        // Sort delegations
        final SortOperation sortOperation = Aggregation.sort(Sort.Direction.ASC, "createdAt");

        // Limit delegations
        final LimitOperation limitOperation = Aggregation.limit(size);

        // Lookup institutions
        final LookupOperation lookupOperationFrom = Aggregation.lookup(COLLECTION_INSTITUTION, "from", "_id", "institution");
        final UnwindOperation unwindOperationFrom = Aggregation.unwind("institution");

        // Aggregate
        final Aggregation aggregation = Aggregation.newAggregation(matchOperation, sortOperation, limitOperation, lookupOperationFrom, unwindOperationFrom);
        return mongoTemplate.aggregate(aggregation, COLLECTION_DELEGATIONS, DelegationInstitutionEntity.class).getMappedResults()
                .stream().map(delegationMapper::convertToDelegationInstitution).toList();
    }

    @Override
    public List<DelegationInstitution> findDelegates(String institutionId, String productId, DelegationType type, Long cursor, int size) {
        final List<Criteria> matchCriterias = getCriterias(institutionId, null, productId, type);
        Optional.ofNullable(cursor).ifPresent(c -> matchCriterias.add(Criteria.where("createdAt")
                .gt(OffsetDateTime.ofInstant(Instant.ofEpochMilli(c), ZoneId.systemDefault()))));

        // Filter delegations
        final MatchOperation matchOperation = Aggregation.match(new Criteria().andOperator(matchCriterias));

        // Sort delegations
        final SortOperation sortOperation = Aggregation.sort(Sort.Direction.ASC, "createdAt");

        // Limit delegations
        final LimitOperation limitOperation = Aggregation.limit(size);

        // Lookup institutions
        final LookupOperation lookupOperationTo = Aggregation.lookup(COLLECTION_INSTITUTION, "to", "_id", "institution");
        final UnwindOperation unwindOperationTo = Aggregation.unwind("institution");

        // Aggregate
        final Aggregation aggregation = Aggregation.newAggregation(matchOperation, sortOperation, limitOperation, lookupOperationTo, unwindOperationTo);
        return mongoTemplate.aggregate(aggregation, COLLECTION_DELEGATIONS, DelegationInstitutionEntity.class).getMappedResults()
                .stream().map(delegationMapper::convertToDelegationInstitution).toList();
    }

}
