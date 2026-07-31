package com.tabibma.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real SMTP/email vendor — .env.example's SMTP_* values are still "changeme"
 * placeholders, same mock-external-vendor pattern as Epic 1's mock TURN provider and Story 5.1's
 * mock CMI gateway. Logs instead of sending.
 */
@Component
public class MockEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(MockEmailSender.class);

    @Override
    public void send(String toEmail, String subject, String body) {
        log.info("[MOCK EMAIL] to={} subject={} body={}", toEmail, subject, body);
    }
}
