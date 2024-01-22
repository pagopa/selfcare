package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.service.UserEventService;
import it.pagopa.selfcare.user.service.UserService;
import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Authenticated
@Path("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserEventService userEventService;


}

