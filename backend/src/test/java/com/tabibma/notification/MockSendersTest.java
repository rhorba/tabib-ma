package com.tabibma.notification;

import org.junit.jupiter.api.Test;

/** Mock senders just log — these tests only prove they don't throw. */
class MockSendersTest {

    @Test
    void mockSmsSender_doesNotThrow() {
        new MockSmsSender().send("+212600000000", "hello");
    }

    @Test
    void mockEmailSender_doesNotThrow() {
        new MockEmailSender().send("p@example.com", "subject", "body");
    }
}
