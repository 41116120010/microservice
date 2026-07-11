package com.daffiqtrie.auth_service.token;

import com.daffiqtrie.auth_service.config.AuthProperties;
import com.daffiqtrie.auth_service.user.AppUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;

    public TokenService(JwtEncoder jwtEncoder, AuthProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public String createToken(AppUser user) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.getUsername())
                .audience(List.of("microservice-api"))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.jwt().expirationMinutes(), ChronoUnit.MINUTES))
                .claim("roles", List.of(user.getRole()))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
