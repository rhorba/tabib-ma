package com.tabibma.booking;

import com.tabibma.booking.dto.AvailabilityExceptionResponse;
import com.tabibma.booking.dto.AvailabilityRuleResponse;
import com.tabibma.booking.dto.AvailabilitySlotResponse;
import com.tabibma.booking.dto.CreateAvailabilityExceptionRequest;
import com.tabibma.booking.dto.CreateAvailabilityRuleRequest;
import com.tabibma.booking.dto.GenerateSlotsRequest;
import com.tabibma.identity.UserContext;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/booking/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/rules")
    public ResponseEntity<AvailabilityRuleResponse> createRule(@AuthenticationPrincipal UserContext principal,
                                                                 @Valid @RequestBody CreateAvailabilityRuleRequest request) {
        AvailabilityRule rule = availabilityService.createRule(principal, request);
        List<UUID> resourceIds = availabilityService.listResourceIdsForRule(rule.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AvailabilityRuleResponse.from(rule, resourceIds));
    }

    @GetMapping("/rules")
    public List<AvailabilityRuleResponse> listMyRules(@AuthenticationPrincipal UserContext principal) {
        return availabilityService.listMyRules(principal).stream()
                .map(rule -> AvailabilityRuleResponse.from(rule, availabilityService.listResourceIdsForRule(rule.getId())))
                .toList();
    }

    @PostMapping("/exceptions")
    public ResponseEntity<AvailabilityExceptionResponse> createException(@AuthenticationPrincipal UserContext principal,
                                                                           @Valid @RequestBody CreateAvailabilityExceptionRequest request) {
        AvailabilityBlockedDate blockedDate = availabilityService.createException(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AvailabilityExceptionResponse.from(blockedDate));
    }

    @GetMapping("/exceptions")
    public List<AvailabilityExceptionResponse> listMyExceptions(@AuthenticationPrincipal UserContext principal) {
        return availabilityService.listMyExceptions(principal).stream().map(AvailabilityExceptionResponse::from).toList();
    }

    @PostMapping("/generate")
    public List<AvailabilitySlotResponse> generateSlots(@AuthenticationPrincipal UserContext principal,
                                                          @RequestBody(required = false) GenerateSlotsRequest request) {
        LocalDate fromDate = request != null ? request.fromDate() : null;
        LocalDate toDate = request != null ? request.toDate() : null;
        return availabilityService.generateSlots(principal, fromDate, toDate).stream()
                .map(AvailabilitySlotResponse::from)
                .toList();
    }

    @GetMapping("/slots")
    public List<AvailabilitySlotResponse> listOpenSlots(@RequestParam UUID doctorProfileId,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return availabilityService.listOpenSlots(doctorProfileId, from, to).stream()
                .map(AvailabilitySlotResponse::from)
                .toList();
    }
}
