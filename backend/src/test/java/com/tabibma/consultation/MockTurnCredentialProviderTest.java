package com.tabibma.consultation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockTurnCredentialProviderTest {

    @Test
    void iceServersFor_returnsAStunOnlyServerRegardlessOfCaller() {
        MockTurnCredentialProvider provider = new MockTurnCredentialProvider();

        List<IceServer> servers = provider.iceServersFor(UUID.randomUUID(), UUID.randomUUID());

        assertThat(servers).hasSize(1);
        assertThat(servers.get(0).urls()).startsWith("stun:");
        assertThat(servers.get(0).username()).isNull();
        assertThat(servers.get(0).credential()).isNull();
    }
}
