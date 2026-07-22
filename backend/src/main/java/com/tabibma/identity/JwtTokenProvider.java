package com.tabibma.identity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Issues and verifies RS256 access tokens per docs/security-tabib-ma.md Section 3. */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(PrivateKey privateKey, PublicKey publicKey,
                             @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMillis(accessTokenExpirationMs))))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    /** Returns empty if the token is missing, malformed, expired, or fails signature verification. */
    public Optional<UserContext> validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            return Optional.of(new UserContext(userId, null, role));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
