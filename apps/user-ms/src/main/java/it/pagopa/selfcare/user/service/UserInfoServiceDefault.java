package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.mapper.UserInfoMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserInfoServiceDefault implements UserInfoService {

    private final UserInfoMapper userInfoMapper;

    @Override
    public Uni<UserInfoResponse> findById(String userId) {
        Uni<UserInfo> userInfo = UserInfo.findById(userId);
        return userInfo
                .onItem().invoke(user -> log.info("Founded userInfo for userId: {}", userId))
                .onItem().transform(userInfoMapper::toResponse);
    }

}
