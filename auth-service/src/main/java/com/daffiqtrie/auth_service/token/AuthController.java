package com.daffiqtrie.auth_service.token;

import com.daffiqtrie.auth_service.config.AuthProperties;
import com.daffiqtrie.auth_service.user.AppUser;
import com.daffiqtrie.auth_service.user.UserRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final List<String> AUTH_MDC_FIELDS = List.of(
            "event_type", "activity", "http_method", "request_path", "status_code",
            "outcome", "principal", "error_type");

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

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new RegistrationConflictException("Username is already registered", username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationConflictException("Email is already registered", username);
        }

        AppUser user = userRepository.save(new AppUser(
                username,
                email,
                passwordEncoder.encode(request.password()),
                "USER"));
        logAuthActivity("register", user.getUsername(), 201, "success", null, "User registration succeeded");
        return new RegisterResponse(user.getUsername(), user.getEmail(), user.getRole());
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        String username = request.username().trim();
        try {
            AppUser user = userRepository.findByUsername(username)
                    .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                    .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
            TokenResponse response = new TokenResponse(
                    tokenService.createToken(user), "Bearer", properties.jwt().expirationMinutes() * 60);
            logAuthActivity("login", user.getUsername(), 200, "success", null, "User login succeeded");
            return response;
        } catch (BadCredentialsException exception) {
            logAuthActivity("login", username, 401, "failure", "authentication", "Invalid username or password");
            throw exception;
        }
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException.class)
    void invalidCredentials() {
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ValidationErrorResponse validationFailed(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.put(
                error.getField(),
                error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
        String activity = request.getRequestURI().endsWith("/login") ? "login" : "register";
        logAuthActivity(activity, null, 400, "failure", "validation",
                "Validation failed for fields: " + String.join(",", errors.keySet()));
        return new ValidationErrorResponse("Validation failed", errors);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(RegistrationConflictException.class)
    void registrationConflict(RegistrationConflictException exception) {
        logAuthActivity("register", exception.username(), 409, "failure", "conflict", exception.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    void registrationDataConflict() {
        logAuthActivity("register", null, 409, "failure", "conflict", "Registration conflict");
    }

    private void logAuthActivity(String activity, String username, int statusCode, String outcome,
            String errorType, String message) {
        try {
            MDC.put("event_type", "user_activity");
            MDC.put("activity", activity);
            MDC.put("http_method", "POST");
            MDC.put("request_path", "/auth/" + activity);
            MDC.put("status_code", Integer.toString(statusCode));
            MDC.put("outcome", outcome);
            if (username != null && !username.isBlank()) {
                MDC.put("principal", username);
            }
            if (errorType != null) {
                MDC.put("error_type", errorType);
            }
            log.info(message);
        } finally {
            AUTH_MDC_FIELDS.forEach(MDC::remove);
        }
    }

    record RegisterRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record RegisterResponse(String username, String email, String role) {
    }

    record ValidationErrorResponse(String message, Map<String, String> errors) {
    }

    record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }

    static class RegistrationConflictException extends RuntimeException {
        private final String username;

        RegistrationConflictException(String message, String username) {
            super(message);
            this.username = username;
        }

        String username() {
            return username;
        }
    }
}
