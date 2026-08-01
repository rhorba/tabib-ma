package com.tabibma.consultation;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface (Architecture doc §3) — a real Twilio Video/Daily.co integration would replace this
 * with the vendor's own room-access token, sidestepping the self-hosted WebSocket relay entirely.
 * Until that vendor spike resolves, JwtSignalingTokenIssuer scopes a short-lived token to exactly
 * one (consultationId, userId) pair so the signaling WebSocket handshake (which cannot carry the
 * normal Authorization bearer header) can still be authenticated.
 */
public interface SignalingTokenIssuer {

    SignalingToken issue(UUID consultationId, UUID userId, Instant expiresAt);

    /** Returns empty if the token is missing, malformed, expired, or fails signature verification. */
    Optional<SignalingTokenClaims> validate(String token);
}
