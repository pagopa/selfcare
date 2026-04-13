package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.onboarding.User;
import it.pagopa.selfcare.onboarding.controller.request.UserDataValidationDto;
import it.pagopa.selfcare.onboarding.controller.request.UserDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNull;

class PnPGUserMapperTest {

    @ParameterizedTest
    @ValueSource(classes = {
            UserDto.class,
            UserDataValidationDto.class
    })
    void toUser_null(Class<?> clazz) {
    final UserResourceMapper userMapper = new UserResourceMapperImpl();
        //given
        //when
        User resource;
        if (UserDto.class.isAssignableFrom(clazz)) {
            resource = userMapper.toUser((UserDto) null);
        } else if (UserDataValidationDto.class.isAssignableFrom(clazz)) {
            resource = userMapper.toUser((UserDataValidationDto) null);
        } else {
            throw new IllegalArgumentException();
        }
        //then
        assertNull(resource);
    }
}