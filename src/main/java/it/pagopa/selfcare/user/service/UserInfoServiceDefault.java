package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.mapper.UserInfoMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

@Slf4j
@ApplicationScoped
public class UserInfoServiceDefault implements UserInfoService {

    @Inject
    private UserInfoMapper userInfoMapper;

    @Override
    public Uni<UserInfoResponse> findById(String id) {
        Uni<UserInfo> userInfo = UserInfo.findById(new ObjectId(id));
        return userInfo.onItem().transform(userInfoMapper::toResponse);
    }

    @Override
    public Uni<UserInfoResponse> findByUserId(String userId) {
        Uni<UserInfo> userInfo = UserInfo.find("userId", userId).firstResult();
        return userInfo.onItem().transform(userInfoMapper::toResponse);
    }

}
