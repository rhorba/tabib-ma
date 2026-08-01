package com.tabibma.consultation.signaling;

import com.tabibma.consultation.SignalingTokenClaims;
import com.tabibma.consultation.SignalingTokenIssuer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * Authenticates the WebSocket handshake with a SignalingTokenIssuer-issued token passed as a
 * query param (browsers cannot set a custom Authorization header on a WebSocket upgrade request).
 * The token was only ever issued to a verified appointment participant at join time
 * (ConsultationService.join), so a valid, unexpired token IS the authorization check here — no
 * second DB round-trip needed.
 */
public class SignalingHandshakeInterceptor implements HandshakeInterceptor {

    static final String ATTR_CONSULTATION_ID = "consultationId";
    static final String ATTR_USER_ID = "userId";

    private final SignalingTokenIssuer signalingTokenIssuer;

    public SignalingHandshakeInterceptor(SignalingTokenIssuer signalingTokenIssuer) {
        this.signalingTokenIssuer = signalingTokenIssuer;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (token == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        Optional<SignalingTokenClaims> claims = signalingTokenIssuer.validate(token);
        if (claims.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(ATTR_CONSULTATION_ID, claims.get().consultationId());
        attributes.put(ATTR_USER_ID, claims.get().userId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
