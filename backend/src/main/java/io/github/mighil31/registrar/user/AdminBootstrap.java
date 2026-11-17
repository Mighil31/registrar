package io.github.mighil31.registrar.user;

import java.time.Clock;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Nobody can grant the first admin role, so it comes from configuration: if no admin exists and
 * {@code registrar.admin.email} is set, that user becomes one (created if needed). Once any admin
 * exists this does nothing, so changing the settings later can't take over an account.
 */
@Component
@EnableConfigurationProperties(AdminProperties.class)
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminProperties admin;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    AdminBootstrap(AdminProperties admin, UserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.admin = admin;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRoles(Role.ADMIN)) {
            return;
        }
        if (!StringUtils.hasText(admin.email())) {
            log.warn("No admin exists. Set registrar.admin.* (REGISTRAR_ADMIN_EMAIL, ...) to create one.");
            return;
        }
        String email = User.normalizeEmail(admin.email());
        User existing = users.findByEmail(email).orElse(null);
        if (existing != null) {
            users.save(existing.withRoles(EnumSet.of(Role.MEMBER, Role.ADMIN), clock.instant()));
            log.info("Made existing user {} the first admin", email);
            return;
        }
        if (!StringUtils.hasText(admin.password()) || admin.password().length() < 12) {
            throw new IllegalStateException("registrar.admin.password must be at least 12 characters");
        }
        User created = User.newMember(
                        StringUtils.hasText(admin.name()) ? admin.name() : "Admin",
                        email,
                        StringUtils.hasText(admin.phoneNumber()) ? admin.phoneNumber() : "0000000000",
                        passwordEncoder.encode(admin.password()),
                        clock.instant())
                .withRoles(EnumSet.of(Role.MEMBER, Role.ADMIN), clock.instant());
        users.insert(created);
        log.info("Created the first admin, {}", email);
    }
}
