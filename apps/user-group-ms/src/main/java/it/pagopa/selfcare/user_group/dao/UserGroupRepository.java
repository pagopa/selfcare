package it.pagopa.selfcare.user_group.dao;

import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserGroupRepository extends MongoRepository<UserGroupEntity, String> {

}
