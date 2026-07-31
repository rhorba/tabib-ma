package com.tabibma.booking;

import com.tabibma.booking.dto.AppointmentResponse;
import com.tabibma.booking.dto.BookAppointmentRequest;
import com.tabibma.identity.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/booking/appointments")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@AuthenticationPrincipal UserContext principal,
                                                     @Valid @RequestBody BookAppointmentRequest request) {
        Appointment appointment = bookingService.bookAndPay(principal, request.availabilitySlotId());
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentResponse.from(appointment));
    }

    @GetMapping
    public List<AppointmentResponse> listMine(@AuthenticationPrincipal UserContext principal) {
        return bookingService.listMyAppointments(principal).stream().map(AppointmentResponse::from).toList();
    }
}
