package com.tabibma.consultation;

import java.time.Instant;

public record SignalingToken(String value, Instant expiresAt) {
}
