package com.tabibma.consultation;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Reuses the same RS256 keypair as the main access-token flow (JwtKeyConfig) rather than
 * standing up a second signing key for a token whose only job is gating one WebSocket handshake. */
@Component
public class JwtSignalingTokenIssuer implements SignalingTokenIssuer {

    private static final String CLAIM_CONSULTATION_ID = "cid";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtSignalingTokenIssuer(PrivateKey jwtPrivateKey, PublicKey jwtPublicKey) {
        this.privateKey = jwtPrivateKey;
        this.publicKey = jwtPublicKey;
    }

    @Override
    public SignalingToken issue(UUID consultationId, UUID userId, Instant expiresAt) {
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_CONSULTATION_ID, consultationId.toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
        return new SignalingToken(token, expiresAt);
    }

    @Override
    public Optional<SignalingTokenClaims> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            UUID consultationId = UUID.fromString(claims.get(CLAIM_CONSULTATION_ID, String.class));
            return Optional.of(new SignalingTokenClaims(consultationId, userId));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
