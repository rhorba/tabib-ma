package com.tabibma.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real SMS vendor — .env.example's SMS_PROVIDER_API_KEY is still a "changeme"
 * placeholder, same mock-external-vendor pattern as Epic 1's mock TURN provider and Story 5.1's
 * mock CMI gateway. Logs instead of sending.
 */
@Component
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void send(String toPhoneNumber, String message) {
        log.info("[MOCK SMS] to={} message={}", toPhoneNumber, message);
    }
}
