package com.tabibma.consultation.signaling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationSignalingHandlerTest {

    @Mock
    private WebSocketSession first;
    @Mock
    private WebSocketSession second;
    @Mock
    private WebSocketSession third;

    private final ConsultationSignalingHandler handler = new ConsultationSignalingHandler();
    private UUID consultationId;

    @BeforeEach
    void setUp() {
        consultationId = UUID.randomUUID();
        session(first, "session-1", consultationId);
        session(second, "session-2", consultationId);
        session(third, "session-3", consultationId);
        lenient().when(first.isOpen()).thenReturn(true);
        lenient().when(second.isOpen()).thenReturn(true);
        lenient().when(third.isOpen()).thenReturn(true);
    }

    private void session(WebSocketSession session, String id, UUID consultationId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SignalingHandshakeInterceptor.ATTR_CONSULTATION_ID, consultationId);
        attributes.put(SignalingHandshakeInterceptor.ATTR_USER_ID, UUID.randomUUID());
        lenient().when(session.getAttributes()).thenReturn(attributes);
        lenient().when(session.getId()).thenReturn(id);
    }

    @Test
    void firstConnection_getsNoPeerJoinedNotification() throws Exception {
        handler.afterConnectionEstablished(first);

        verify(first, never()).sendMessage(any());
    }

    @Test
    void secondConnection_notifiesTheFirstThatAPeerJoined() throws Exception {
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);

        verify(first).sendMessage(new TextMessage("{\"type\":\"peer-joined\"}"));
        verify(second, never()).sendMessage(any());
    }

    @Test
    void aThirdConnection_isRejectedWithPolicyViolation() throws Exception {
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);

        handler.afterConnectionEstablished(third);

        verify(third).close(eq(CloseStatus.POLICY_VIOLATION.withReason("Consultation room is full.")));
    }

    @Test
    void handleTextMessage_relaysToTheOtherParticipantOnly() throws Exception {
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);

        handler.handleMessage(first, new TextMessage("{\"type\":\"offer\"}"));

        verify(second).sendMessage(new TextMessage("{\"type\":\"offer\"}"));
        verify(first, never()).sendMessage(new TextMessage("{\"type\":\"offer\"}"));
    }

    @Test
    void afterConnectionClosed_notifiesTheRemainingPeer() throws Exception {
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);

        handler.afterConnectionClosed(second, CloseStatus.NORMAL);

        verify(first).sendMessage(new TextMessage("{\"type\":\"peer-left\"}"));
    }

    @Test
    void afterConnectionClosed_doesNotFailWhenTheRoomIsAlreadyGone() {
        org.assertj.core.api.Assertions.assertThatCode(() -> handler.afterConnectionClosed(first, CloseStatus.NORMAL))
                .doesNotThrowAnyException();
    }
}
