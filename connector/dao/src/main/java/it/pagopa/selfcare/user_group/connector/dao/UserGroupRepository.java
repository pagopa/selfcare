package it.pagopa.selfcare.user_group.connector.dao;

import it.pagopa.selfcare.user_group.connector.dao.model.UserGroupEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserGroupRepository extends MongoRepository<UserGroupEntity, String> {

}
