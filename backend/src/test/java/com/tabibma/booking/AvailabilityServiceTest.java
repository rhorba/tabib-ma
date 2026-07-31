package com.tabibma.booking;

import com.tabibma.booking.dto.CreateAvailabilityExceptionRequest;
import com.tabibma.booking.dto.CreateAvailabilityRuleRequest;
import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private AvailabilityRuleRepository availabilityRuleRepository;
    @Mock
    private AvailabilityBlockedDateRepository availabilityBlockedDateRepository;
    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;

    private AvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new AvailabilityService(doctorProfileRepository, availabilityRuleRepository,
                availabilityBlockedDateRepository, availabilitySlotRepository);
    }

    private static DoctorProfile profile(UUID userId) {
        return new DoctorProfile(userId, "Cardiology", "bio", BigDecimal.TEN, "Rabat");
    }

    @Test
    void createRule_rejectsNonDoctorRole() {
        UserContext patient = new UserContext(UUID.randomUUID(), "p@example.com", Role.PATIENT);
        CreateAvailabilityRuleRequest request = new CreateAvailabilityRuleRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, LocationType.IN_PERSON, null);

        assertThatThrownBy(() -> service.createRule(patient, request)).isInstanceOf(ForbiddenException.class);
        verify(availabilityRuleRepository, never()).save(any());
    }

    @Test
    void createRule_rejectsWhenNoProfile() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.empty());
        CreateAvailabilityRuleRequest request = new CreateAvailabilityRuleRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, LocationType.IN_PERSON, null);

        assertThatThrownBy(() -> service.createRule(doctor, request)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRule_rejectsWhenEndBeforeStart() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile(doctor.userId())));
        CreateAvailabilityRuleRequest request = new CreateAvailabilityRuleRequest(
                DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(9, 0), 30, LocationType.IN_PERSON, null);

        assertThatThrownBy(() -> service.createRule(doctor, request)).isInstanceOf(ValidationException.class);
    }

    @Test
    void createRule_savesValidRule() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile(doctor.userId())));
        when(availabilityRuleRepository.save(any(AvailabilityRule.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateAvailabilityRuleRequest request = new CreateAvailabilityRuleRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, LocationType.IN_PERSON, null);

        AvailabilityRule saved = service.createRule(doctor, request);

        assertThat(saved.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void createException_rejectsPastDate() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile(doctor.userId())));
        CreateAvailabilityExceptionRequest request = new CreateAvailabilityExceptionRequest(
                LocalDate.now(AvailabilityService.CLINIC_ZONE).minusDays(1), "holiday");

        assertThatThrownBy(() -> service.createException(doctor, request)).isInstanceOf(ValidationException.class);
    }

    @Test
    void createException_rejectsDuplicate() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        DoctorProfile profile = profile(doctor.userId());
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile));
        LocalDate date = LocalDate.now(AvailabilityService.CLINIC_ZONE).plusDays(5);
        when(availabilityBlockedDateRepository.existsByDoctorProfileIdAndExceptionDate(profile.getId(), date)).thenReturn(true);
        CreateAvailabilityExceptionRequest request = new CreateAvailabilityExceptionRequest(date, "holiday");

        assertThatThrownBy(() -> service.createException(doctor, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void createException_savesValid() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        DoctorProfile profile = profile(doctor.userId());
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile));
        LocalDate date = LocalDate.now(AvailabilityService.CLINIC_ZONE).plusDays(5);
        when(availabilityBlockedDateRepository.existsByDoctorProfileIdAndExceptionDate(profile.getId(), date)).thenReturn(false);
        when(availabilityBlockedDateRepository.save(any(AvailabilityBlockedDate.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateAvailabilityExceptionRequest request = new CreateAvailabilityExceptionRequest(date, "holiday");

        AvailabilityBlockedDate saved = service.createException(doctor, request);

        assertThat(saved.getExceptionDate()).isEqualTo(date);
        assertThat(saved.getReason()).isEqualTo("holiday");
    }

    @Test
    void generateSlots_rejectsWhenToBeforeFrom() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile(doctor.userId())));
        LocalDate from = LocalDate.now(AvailabilityService.CLINIC_ZONE);

        assertThatThrownBy(() -> service.generateSlots(doctor, from, from))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void generateSlots_rejectsWhenRangeTooLarge() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile(doctor.userId())));
        LocalDate from = LocalDate.now(AvailabilityService.CLINIC_ZONE);

        assertThatThrownBy(() -> service.generateSlots(doctor, from, from.plusDays(200)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void generateSlots_returnsEmptyWhenNoActiveRules() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        DoctorProfile profile = profile(doctor.userId());
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile));
        when(availabilityRuleRepository.findAllByDoctorProfileIdAndActiveTrue(profile.getId())).thenReturn(List.of());
        LocalDate from = LocalDate.now(AvailabilityService.CLINIC_ZONE);

        List<AvailabilitySlot> result = service.generateSlots(doctor, from, from.plusDays(7));

        assertThat(result).isEmpty();
        verify(availabilitySlotRepository, never()).saveAll(any());
    }

    @Test
    void generateSlots_skipsBlockedDateAndDeduplicatesExistingSlots() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        DoctorProfile profile = profile(doctor.userId());
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile));

        // Find the next two occurrences of MONDAY so the test is deterministic regardless of "today".
        LocalDate from = LocalDate.now(AvailabilityService.CLINIC_ZONE);
        LocalDate firstMonday = from.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (firstMonday.isBefore(from)) {
            firstMonday = firstMonday.plusWeeks(1);
        }
        LocalDate secondMonday = firstMonday.plusWeeks(1);
        LocalDate to = secondMonday.plusDays(1);

        AvailabilityRule rule = new AvailabilityRule(profile.getId(), DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), 30, LocationType.IN_PERSON, null);
        when(availabilityRuleRepository.findAllByDoctorProfileIdAndActiveTrue(profile.getId())).thenReturn(List.of(rule));

        AvailabilityBlockedDate blocked = new AvailabilityBlockedDate(profile.getId(), firstMonday, "holiday");
        when(availabilityBlockedDateRepository.findAllByDoctorProfileId(profile.getId())).thenReturn(List.of(blocked));

        // Pretend one of the second Monday's two slots (09:00-09:30) was already generated.
        java.time.Instant existingStart = java.time.ZonedDateTime.of(secondMonday, LocalTime.of(9, 0),
                AvailabilityService.CLINIC_ZONE).toInstant();
        when(availabilitySlotRepository.findStartTimesBetween(any(), any(), any())).thenReturn(Set.of(existingStart));
        when(availabilitySlotRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<AvailabilitySlot> result = service.generateSlots(doctor, from, to);

        // Only the second Monday's 09:30-10:00 slot should be newly generated: the first Monday is
        // blocked, and the second Monday's 09:00-09:30 slot already exists.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartsAt()).isEqualTo(
                java.time.ZonedDateTime.of(secondMonday, LocalTime.of(9, 30), AvailabilityService.CLINIC_ZONE).toInstant());
    }

    @Test
    void listOpenSlots_delegatesToRepository() {
        UUID doctorProfileId = UUID.randomUUID();
        java.time.Instant from = java.time.Instant.now();
        java.time.Instant to = from.plusSeconds(3600);
        AvailabilitySlot slot = new AvailabilitySlot(doctorProfileId, from, to, LocationType.IN_PERSON, null);
        when(availabilitySlotRepository.findAllByDoctorProfileIdAndStartsAtBetweenAndBookedFalseOrderByStartsAt(
                doctorProfileId, from, to)).thenReturn(List.of(slot));

        assertThat(service.listOpenSlots(doctorProfileId, from, to)).containsExactly(slot);
    }
}
