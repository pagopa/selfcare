package it.pagopa.selfcare.user_group.controller;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class DummyController {

    public Object notVoidMethod() {
        return new Object();
    }

    public void voidMethod() {
    }
}
