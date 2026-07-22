package com.tabibma.identity.dto;

import com.tabibma.identity.Role;
import com.tabibma.identity.User;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        Role role,
        String firstName,
        String lastName
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole(), user.getFirstName(), user.getLastName());
    }
}
