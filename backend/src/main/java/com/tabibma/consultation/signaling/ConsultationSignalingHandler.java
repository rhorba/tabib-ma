package com.tabibma.consultation.signaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bare relay for WebRTC signaling (SDP offer/answer, ICE candidates): forwards whatever a peer
 * sends, verbatim, to the other participant in the same consultation room. Message shape/protocol
 * is entirely the frontend's concern (Batch 4) — this handler is content-agnostic except for the
 * small "peer-joined"/"peer-left" system messages it sends itself so each side knows when to start
 * the WebRTC offer/answer exchange. Exactly 2 participants per room (patient + doctor); a 3rd
 * connection attempt is rejected.
 */
@Component
public class ConsultationSignalingHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ConsultationSignalingHandler.class);
    private static final int MAX_PARTICIPANTS_PER_ROOM = 2;

    private final Map<UUID, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID consultationId = consultationId(session);
        Set<WebSocketSession> room = rooms.computeIfAbsent(consultationId, id -> ConcurrentHashMap.newKeySet());
        if (room.size() >= MAX_PARTICIPANTS_PER_ROOM) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Consultation room is full."));
            return;
        }
        boolean peerAlreadyPresent = !room.isEmpty();
        room.add(session);
        if (peerAlreadyPresent) {
            broadcast(room, session, "{\"type\":\"peer-joined\"}");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Set<WebSocketSession> room = rooms.get(consultationId(session));
        if (room != null) {
            broadcast(room, session, message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID consultationId = consultationId(session);
        Set<WebSocketSession> room = rooms.get(consultationId);
        if (room == null) {
            return;
        }
        room.remove(session);
        if (room.isEmpty()) {
            rooms.remove(consultationId);
        } else {
            broadcast(room, session, "{\"type\":\"peer-left\"}");
        }
    }

    private void broadcast(Set<WebSocketSession> room, WebSocketSession sender, String payload) {
        for (WebSocketSession peer : room) {
            if (peer.getId().equals(sender.getId()) || !peer.isOpen()) {
                continue;
            }
            try {
                peer.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.warn("Failed to relay signaling message in consultation {}", consultationId(sender), e);
            }
        }
    }

    private UUID consultationId(WebSocketSession session) {
        return (UUID) session.getAttributes().get(SignalingHandshakeInterceptor.ATTR_CONSULTATION_ID);
    }
}
