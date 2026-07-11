package com.daffiqtrie.auth_service.token;

import com.daffiqtrie.auth_service.config.AuthProperties;
import com.daffiqtrie.auth_service.user.AppUser;
import com.daffiqtrie.auth_service.user.UserRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthProperties properties;
    private final RSAKey rsaKey;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
            AuthProperties properties, RSAKey rsaKey) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.properties = properties;
        this.rsaKey = rsaKey;
    }

    @PostMapping("/auth/token")
    public TokenResponse token(@Valid @RequestBody LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        return new TokenResponse(tokenService.createToken(user), "Bearer", properties.jwt().expirationMinutes() * 60);
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @org.springframework.web.bind.annotation.ExceptionHandler(BadCredentialsException.class)
    void invalidCredentials() {
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
