package com.tabibma.identity;

import java.util.UUID;

/** The authenticated principal, resolved from the JWT claims — never raw IDs trusted from the client. */
public record UserContext(UUID userId, String email, Role role) {
}
