package com.tabibma.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Generates an ephemeral RSA keypair for tests instead of requiring committed key material
 * (a committed PEM, even a fake test-only one, would trip the gitleaks secrets scan in CI —
 * see docs/devops-tabib-ma.md Section 8). Active only under the "test" profile; JwtKeyConfig
 * (the production equivalent) is disabled under that same profile.
 */
@TestConfiguration
@Profile("test")
public class TestJwtKeyConfig {

    @Bean
    public KeyPair testJwtKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Bean
    public PrivateKey jwtPrivateKey(KeyPair testJwtKeyPair) {
        return testJwtKeyPair.getPrivate();
    }

    @Bean
    public PublicKey jwtPublicKey(KeyPair testJwtKeyPair) {
        return testJwtKeyPair.getPublic();
    }
}
