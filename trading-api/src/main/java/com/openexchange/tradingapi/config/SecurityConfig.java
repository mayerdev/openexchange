package com.openexchange.tradingapi.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}")
    private String introspectionUri;

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
    private String clientSecret;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public ReactiveOpaqueTokenIntrospector introspector() {
        var webClient = WebClient.builder()
                .defaultHeaders(h -> h.setBasicAuth(clientId, clientSecret))
                .build();

        return token -> webClient.post()
                .uri(introspectionUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("token", token))
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        Mono.error(new OAuth2IntrospectionException("Introspection endpoint returned " + response.statusCode()))
                )
                .bodyToMono(Map.class)
                .flatMap(body -> {
                    if (!Boolean.TRUE.equals(body.get("active"))) {
                        return Mono.error(new OAuth2IntrospectionException("Token is not active"));
                    }

                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) body.getOrDefault("roles", List.of());

                    var scopeAuthorities = Stream.of(
                                    String.valueOf(body.getOrDefault("scope", ""))
                                            .split(" ")
                            )
                            .filter(s -> !s.isBlank())
                            .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                            .toList();

                    var roleAuthorities = roles.stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                            .toList();

                    List<GrantedAuthority> authorities = Stream.concat(
                            scopeAuthorities.stream(), roleAuthorities.stream()
                    ).collect(Collectors.toList());

                    @SuppressWarnings("unchecked")
                    Map<String, Object> attrs = new HashMap<>((Map<String, Object>) body);

                    if (attrs.get("exp") instanceof Number n) {
                        attrs.put("exp", Instant.ofEpochSecond(n.longValue()));
                    }

                    if (attrs.get("iat") instanceof Number n) {
                        attrs.put("iat", Instant.ofEpochSecond(n.longValue()));
                    }

                    String name = (String) attrs.getOrDefault("sub", "");

                    return Mono.just(
                            (OAuth2AuthenticatedPrincipal) new DefaultOAuth2AuthenticatedPrincipal(name, attrs, authorities)
                    );
                });
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .opaqueToken(opaqueToken -> {
                        })
                        .authenticationEntryPoint((exchange, ex) -> writeError(exchange, ex.getMessage()))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> writeError(exchange, e.getMessage()))
                )
                .build();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(Mono.fromCallable(() -> {
            byte[] body = objectMapper.writeValueAsBytes(
                    Map.of("error", "unauthorized", "message", message)
            );

            return exchange.getResponse().bufferFactory().wrap(body);
        }));
    }
}
