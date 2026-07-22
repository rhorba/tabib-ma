package com.tabibma.identity;

import com.tabibma.identity.dto.UserSummaryResponse;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Story 1.4 (Clinic Admin & Platform Admin roles): proves the RBAC path-prefix separation
 * defined in SecurityConfig. Clinic-wide/platform-wide *data* views (booking volume, disputes,
 * health metrics) belong to the `clinic` and `admin` modules once Epics 2/8/10 exist —
 * this controller only demonstrates that the two admin roles are distinct, enforced, first-class
 * identities, which is what Epic 1 can honestly deliver this sprint.
 */
@RestController
public class AdminAccessController {

    private final UserRepository userRepository;

    public AdminAccessController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/v1/admin/platform/me")
    public UserSummaryResponse platformAdminMe(@AuthenticationPrincipal UserContext principal) {
        return me(principal);
    }

    @GetMapping("/api/v1/admin/clinic/me")
    public UserSummaryResponse clinicAdminMe(@AuthenticationPrincipal UserContext principal) {
        return me(principal);
    }

    private UserSummaryResponse me(UserContext principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("User not found."));
        return UserSummaryResponse.from(user);
    }
}
