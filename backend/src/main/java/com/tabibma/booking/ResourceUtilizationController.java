package com.tabibma.booking;

import com.tabibma.booking.dto.ResourceUtilizationResponse;
import com.tabibma.identity.UserContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Same URL prefix as ClinicDashboardController's other /api/v1/clinic/clinics endpoints — see
 * ResourceUtilizationService's class javadoc for why this lives in the booking package instead. */
@RestController
@RequestMapping("/api/v1/clinic/clinics/resources")
public class ResourceUtilizationController {

    private final ResourceUtilizationService resourceUtilizationService;

    public ResourceUtilizationController(ResourceUtilizationService resourceUtilizationService) {
        this.resourceUtilizationService = resourceUtilizationService;
    }

    @GetMapping("/utilization")
    public List<ResourceUtilizationResponse> getUtilization(@AuthenticationPrincipal UserContext principal) {
        return resourceUtilizationService.getUtilization(principal);
    }
}
