package com.tabibma.consultation;

import java.util.List;
import java.util.UUID;

/**
 * Strategy interface (Architecture doc §4) — swaps a managed TURN vendor (Twilio Video or
 * Daily.co, per docs/stories-tabib-ma.md Story 6.1) in without touching ConsultationService.
 * The Sprint 2 vendor spike never resolved to a real vendor (.logs/decisions.md), so this is
 * still backed by a mock implementation, same pattern as PaymentGateway/SmsSender/EmailSender.
 */
public interface TurnCredentialProvider {

    List<IceServer> iceServersFor(UUID consultationId, UUID userId);
}
