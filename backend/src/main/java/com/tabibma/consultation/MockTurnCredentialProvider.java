package com.tabibma.consultation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Stands in for a real Twilio Video / Daily.co TURN allocation until a vendor is picked and real
 * credentials exist — .env.example's WEBRTC_TURN_* are still "changeme" placeholders, same
 * mock-external-vendor pattern as MockCmiPaymentGatewayAdapter. Returns Google's public STUN
 * server only: sufficient for NAT traversal on most residential/office networks, but calls
 * between two peers both behind symmetric NAT (common on mobile carrier networks) will fail to
 * connect without a real TURN relay — a known limitation until the vendor spike resolves.
 */
@Component
public class MockTurnCredentialProvider implements TurnCredentialProvider {

    private static final List<IceServer> STUN_ONLY = List.of(IceServer.stun("stun:stun.l.google.com:19302"));

    @Override
    public List<IceServer> iceServersFor(UUID consultationId, UUID userId) {
        return STUN_ONLY;
    }
}
