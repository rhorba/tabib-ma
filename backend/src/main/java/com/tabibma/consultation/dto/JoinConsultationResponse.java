package com.tabibma.consultation.dto;

import com.tabibma.consultation.IceServer;
import com.tabibma.consultation.ConsultationService.JoinResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JoinConsultationResponse(
        UUID consultationId,
        String signalingToken,
        Instant signalingTokenExpiresAt,
        List<IceServer> iceServers
) {
    public static JoinConsultationResponse from(JoinResult result) {
        return new JoinConsultationResponse(result.consultation().getId(), result.signalingToken().value(),
                result.signalingToken().expiresAt(), result.iceServers());
    }
}
