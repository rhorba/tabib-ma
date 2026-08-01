package com.tabibma.consultation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSignalingTokenIssuerTest {

    private JwtSignalingTokenIssuer issuer;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        issuer = new JwtSignalingTokenIssuer(keyPair.getPrivate(), keyPair.getPublic());
    }

    @Test
    void issueThenValidate_roundTripsTheConsultationAndUserIds() {
        UUID consultationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        SignalingToken token = issuer.issue(consultationId, userId, Instant.now().plusSeconds(3600));
        Optional<SignalingTokenClaims> claims = issuer.validate(token.value());

        assertThat(claims).isPresent();
        assertThat(claims.get().consultationId()).isEqualTo(consultationId);
        assertThat(claims.get().userId()).isEqualTo(userId);
    }

    @Test
    void validate_emptyForAnExpiredToken() {
        SignalingToken token = issuer.issue(UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().minus(1, ChronoUnit.SECONDS));

        assertThat(issuer.validate(token.value())).isEmpty();
    }

    @Test
    void validate_emptyForATokenSignedByADifferentKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair otherKeyPair = generator.generateKeyPair();
        JwtSignalingTokenIssuer otherIssuer = new JwtSignalingTokenIssuer(otherKeyPair.getPrivate(), otherKeyPair.getPublic());
        SignalingToken token = otherIssuer.issue(UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(3600));

        assertThat(issuer.validate(token.value())).isEmpty();
    }

    @Test
    void validate_emptyForGarbageInput() {
        assertThat(issuer.validate("not-a-jwt")).isEmpty();
    }
}
