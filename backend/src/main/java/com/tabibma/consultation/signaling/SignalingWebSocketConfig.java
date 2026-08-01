package com.tabibma.consultation.signaling;

import com.tabibma.consultation.SignalingTokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class SignalingWebSocketConfig implements WebSocketConfigurer {

    private final ConsultationSignalingHandler handler;
    private final SignalingTokenIssuer signalingTokenIssuer;
    private final String allowedOrigins;

    public SignalingWebSocketConfig(ConsultationSignalingHandler handler, SignalingTokenIssuer signalingTokenIssuer,
                                     @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.handler = handler;
        this.signalingTokenIssuer = signalingTokenIssuer;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/consultations")
                .addInterceptors(new SignalingHandshakeInterceptor(signalingTokenIssuer))
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}
