package io.github.mighil31.registrar;

import org.springframework.boot.SpringApplication;

/** {@code mvn spring-boot:test-run}: the app on a throwaway MongoDB, no compose needed. */
public class TestRegistrarApplication {

    public static void main(String[] args) {
        SpringApplication.from(RegistrarApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
