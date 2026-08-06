package com.tabibma.admin;

import com.tabibma.admin.dto.PlatformHealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Story 10.3. Path lives under /api/v1/admin/platform, already restricted to PLATFORM_ADMIN by
 * SecurityConfig (same convention as AdminDisputeController/AdminAppointmentActionController). */
@RestController
@RequestMapping("/api/v1/admin/platform/health")
public class AdminDashboardController {

    private final PlatformHealthService platformHealthService;

    public AdminDashboardController(PlatformHealthService platformHealthService) {
        this.platformHealthService = platformHealthService;
    }

    @GetMapping
    public PlatformHealthResponse getHealth() {
        return platformHealthService.getHealth();
    }
}
