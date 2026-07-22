package com.tabibma.identity.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInMs
) {
}
