package com.daffiqtrie.auth_service.user;

import com.daffiqtrie.auth_service.config.AuthProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BootstrapAdmin implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;

    public BootstrapAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (StringUtils.hasText(properties.bootstrapAdmin().password())
                && userRepository.findByUsername(properties.bootstrapAdmin().username()).isEmpty()) {
            userRepository.save(new AppUser(properties.bootstrapAdmin().username(),
                    passwordEncoder.encode(properties.bootstrapAdmin().password()), "ADMIN"));
        }
    }
}
