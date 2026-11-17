package io.github.mighil31.registrar.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The first admin, created at startup when there is none. All blank: no admin is created. */
@ConfigurationProperties("registrar.admin")
public record AdminProperties(String email, String password, String name, String phoneNumber) {}
