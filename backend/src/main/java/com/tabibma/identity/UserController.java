package com.tabibma.identity;

import com.tabibma.identity.dto.UserSummaryResponse;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserSummaryResponse me(@AuthenticationPrincipal UserContext principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("User not found."));
        return UserSummaryResponse.from(user);
    }
}
