package com.tabibma.consultation;

import java.util.UUID;

public record SignalingTokenClaims(UUID consultationId, UUID userId) {
}
