package com.tabibma.consultation.signaling;

import com.tabibma.consultation.SignalingTokenClaims;
import com.tabibma.consultation.SignalingTokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignalingHandshakeInterceptorTest {

    @Mock
    private SignalingTokenIssuer signalingTokenIssuer;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private WebSocketHandler wsHandler;

    private SignalingHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new SignalingHandshakeInterceptor(signalingTokenIssuer);
    }

    @Test
    void beforeHandshake_rejectsWhenTokenQueryParamIsMissing() {
        when(request.getURI()).thenReturn(URI.create("wss://host/ws/consultations"));

        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(allowed).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_rejectsWhenTheTokenFailsValidation() {
        when(request.getURI()).thenReturn(URI.create("wss://host/ws/consultations?token=bad"));
        when(signalingTokenIssuer.validate("bad")).thenReturn(Optional.empty());

        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, new HashMap<>());

        assertThat(allowed).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void beforeHandshake_acceptsAndStoresClaimsForAValidToken() {
        UUID consultationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(request.getURI()).thenReturn(URI.create("wss://host/ws/consultations?token=good"));
        when(signalingTokenIssuer.validate("good"))
                .thenReturn(Optional.of(new SignalingTokenClaims(consultationId, userId)));
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes.get(SignalingHandshakeInterceptor.ATTR_CONSULTATION_ID)).isEqualTo(consultationId);
        assertThat(attributes.get(SignalingHandshakeInterceptor.ATTR_USER_ID)).isEqualTo(userId);
    }
}
