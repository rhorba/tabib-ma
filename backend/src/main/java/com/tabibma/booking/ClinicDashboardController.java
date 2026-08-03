package com.tabibma.booking;

import com.tabibma.booking.dto.ClinicDashboardResponse;
import com.tabibma.identity.UserContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Same URL prefix as ClinicController's other /api/v1/clinic/clinics endpoints — see
 * ClinicDashboardService's class javadoc for why this lives in the booking package instead. */
@RestController
@RequestMapping("/api/v1/clinic/clinics")
public class ClinicDashboardController {

    private final ClinicDashboardService clinicDashboardService;

    public ClinicDashboardController(ClinicDashboardService clinicDashboardService) {
        this.clinicDashboardService = clinicDashboardService;
    }

    @GetMapping("/dashboard")
    public ClinicDashboardResponse getDashboard(@AuthenticationPrincipal UserContext principal) {
        return clinicDashboardService.getDashboard(principal);
    }
}
