package com.daffiqtrie.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AuthProperties(Jwt jwt, BootstrapAdmin bootstrapAdmin) {

    public record Jwt(String issuer, long expirationMinutes) {
    }

    public record BootstrapAdmin(String username, String email, String password) {
    }
}
