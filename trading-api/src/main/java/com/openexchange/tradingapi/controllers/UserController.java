package com.openexchange.tradingapi.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/me")
    public Mono<Map<String, Object>> me(Authentication authentication) {
        BearerTokenAuthentication token = (BearerTokenAuthentication) authentication;
        Map<String, Object> attrs = token.getTokenAttributes();

        return Mono.just(attrs);
    }
}
