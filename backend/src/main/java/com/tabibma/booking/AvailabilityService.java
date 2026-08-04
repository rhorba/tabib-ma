package com.tabibma.booking;

import com.tabibma.booking.dto.CreateAvailabilityExceptionRequest;
import com.tabibma.booking.dto.CreateAvailabilityRuleRequest;
import com.tabibma.clinic.ClinicResource;
import com.tabibma.clinic.ClinicResourceRepository;
import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AvailabilityService {

    static final ZoneId CLINIC_ZONE = ZoneId.of("Africa/Casablanca");
    private static final long MAX_GENERATION_DAYS = 90;

    private final DoctorProfileRepository doctorProfileRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final AvailabilityBlockedDateRepository availabilityBlockedDateRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final ClinicResourceRepository clinicResourceRepository;
    private final AvailabilityRuleResourceRepository availabilityRuleResourceRepository;
    private final AvailabilitySlotResourceRepository availabilitySlotResourceRepository;

    public AvailabilityService(DoctorProfileRepository doctorProfileRepository,
                                AvailabilityRuleRepository availabilityRuleRepository,
                                AvailabilityBlockedDateRepository availabilityBlockedDateRepository,
                                AvailabilitySlotRepository availabilitySlotRepository,
                                ClinicResourceRepository clinicResourceRepository,
                                AvailabilityRuleResourceRepository availabilityRuleResourceRepository,
                                AvailabilitySlotResourceRepository availabilitySlotResourceRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.availabilityRuleRepository = availabilityRuleRepository;
        this.availabilityBlockedDateRepository = availabilityBlockedDateRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.clinicResourceRepository = clinicResourceRepository;
        this.availabilityRuleResourceRepository = availabilityRuleResourceRepository;
        this.availabilitySlotResourceRepository = availabilitySlotResourceRepository;
    }

    @Transactional
    public AvailabilityRule createRule(UserContext principal, CreateAvailabilityRuleRequest request) {
        DoctorProfile profile = ownDoctorProfileOrThrow(principal);
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ValidationException("endTime must be after startTime.");
        }
        Set<UUID> resourceIds = request.resourceIds() != null ? request.resourceIds() : Set.of();
        if (!resourceIds.isEmpty()) {
            if (request.locationType() != LocationType.IN_PERSON) {
                throw new ValidationException("Resources can only be required for IN_PERSON availability.");
            }
            if (request.clinicId() == null) {
                throw new ValidationException("clinicId is required when specifying resources.");
            }
            for (UUID resourceId : resourceIds) {
                ClinicResource resource = clinicResourceRepository.findById(resourceId)
                        .orElseThrow(() -> new NotFoundException("Resource not found: " + resourceId));
                if (!resource.getClinicId().equals(request.clinicId())) {
                    throw new NotFoundException("Resource not found: " + resourceId);
                }
                if (!resource.isActive()) {
                    throw new ValidationException("Resource is not active: " + resourceId);
                }
            }
        }
        AvailabilityRule rule = new AvailabilityRule(profile.getId(), request.dayOfWeek(), request.startTime(),
                request.endTime(), request.slotDurationMinutes(), request.locationType(), request.clinicId());
        AvailabilityRule saved = availabilityRuleRepository.save(rule);
        for (UUID resourceId : resourceIds) {
            availabilityRuleResourceRepository.save(new AvailabilityRuleResource(saved.getId(), resourceId));
        }
        return saved;
    }

    public List<UUID> listResourceIdsForRule(UUID availabilityRuleId) {
        return availabilityRuleResourceRepository.findAllByAvailabilityRuleId(availabilityRuleId).stream()
                .map(AvailabilityRuleResource::getResourceId)
                .toList();
    }

    public List<AvailabilityRule> listMyRules(UserContext principal) {
        DoctorProfile profile = ownDoctorProfileOrThrow(principal);
        return availabilityRuleRepository.findAllByDoctorProfileId(profile.getId());
    }

    @Transactional
    public AvailabilityBlockedDate createException(UserContext principal, CreateAvailabilityExceptionRequest request) {
        DoctorProfile profile = ownDoctorProfileOrThrow(principal);
        if (request.exceptionDate().isBefore(LocalDate.now(CLINIC_ZONE))) {
            throw new ValidationException("exceptionDate cannot be in the past.");
        }
        if (availabilityBlockedDateRepository.existsByDoctorProfileIdAndExceptionDate(profile.getId(), request.exceptionDate())) {
            throw new ConflictException("This date is already marked as an exception.");
        }
        AvailabilityBlockedDate blockedDate = new AvailabilityBlockedDate(profile.getId(), request.exceptionDate(), request.reason());
        return availabilityBlockedDateRepository.save(blockedDate);
    }

    public List<AvailabilityBlockedDate> listMyExceptions(UserContext principal) {
        DoctorProfile profile = ownDoctorProfileOrThrow(principal);
        return availabilityBlockedDateRepository.findAllByDoctorProfileId(profile.getId());
    }

    @Transactional
    public List<AvailabilitySlot> generateSlots(UserContext principal, LocalDate fromDate, LocalDate toDate) {
        DoctorProfile profile = ownDoctorProfileOrThrow(principal);
        LocalDate from = fromDate != null ? fromDate : LocalDate.now(CLINIC_ZONE);
        LocalDate to = toDate != null ? toDate : from.plusDays(30);
        if (!to.isAfter(from)) {
            throw new ValidationException("toDate must be after fromDate.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_GENERATION_DAYS) {
            throw new ValidationException("Cannot generate more than " + MAX_GENERATION_DAYS + " days of slots at once.");
        }

        List<AvailabilityRule> rules = availabilityRuleRepository.findAllByDoctorProfileIdAndActiveTrue(profile.getId());
        if (rules.isEmpty()) {
            return List.of();
        }

        Set<LocalDate> blockedDates = new HashSet<>();
        for (AvailabilityBlockedDate blocked : availabilityBlockedDateRepository.findAllByDoctorProfileId(profile.getId())) {
            LocalDate date = blocked.getExceptionDate();
            if (!date.isBefore(from) && date.isBefore(to)) {
                blockedDates.add(date);
            }
        }

        Instant fromInstant = from.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant toInstant = to.atStartOfDay(CLINIC_ZONE).toInstant();
        Set<Instant> existingStarts = new HashSet<>(
                availabilitySlotRepository.findStartTimesBetween(profile.getId(), fromInstant, toInstant));

        List<AvailabilitySlot> generated = new ArrayList<>();
        List<UUID> ruleIdPerGeneratedSlot = new ArrayList<>();
        for (LocalDate date = from; date.isBefore(to); date = date.plusDays(1)) {
            if (blockedDates.contains(date)) {
                continue;
            }
            for (AvailabilityRule rule : rules) {
                if (rule.getDayOfWeek() != date.getDayOfWeek()) {
                    continue;
                }
                LocalTime cursor = rule.getStartTime();
                while (!cursor.plusMinutes(rule.getSlotDurationMinutes()).isAfter(rule.getEndTime())) {
                    LocalTime slotEnd = cursor.plusMinutes(rule.getSlotDurationMinutes());
                    Instant startsAt = ZonedDateTime.of(date, cursor, CLINIC_ZONE).toInstant();
                    Instant endsAt = ZonedDateTime.of(date, slotEnd, CLINIC_ZONE).toInstant();
                    if (existingStarts.add(startsAt)) {
                        generated.add(new AvailabilitySlot(profile.getId(), startsAt, endsAt,
                                rule.getLocationType(), rule.getClinicId()));
                        ruleIdPerGeneratedSlot.add(rule.getId());
                    }
                    cursor = slotEnd;
                }
            }
        }
        List<AvailabilitySlot> saved = availabilitySlotRepository.saveAll(generated);
        copyRequiredResourcesOntoGeneratedSlots(saved, ruleIdPerGeneratedSlot);
        return saved;
    }

    /** Denormalizes each generating rule's required resources onto its slots (Story 8.2 Batch 3),
     * the same way {@code locationType}/{@code clinicId} are already copied at generation time —
     * so booking-time conflict checks never need to look the originating rule back up. */
    private void copyRequiredResourcesOntoGeneratedSlots(List<AvailabilitySlot> savedSlots, List<UUID> ruleIdPerSlot) {
        Map<UUID, List<UUID>> resourceIdsByRule = new HashMap<>();
        for (int i = 0; i < savedSlots.size(); i++) {
            UUID ruleId = ruleIdPerSlot.get(i);
            List<UUID> resourceIds = resourceIdsByRule.computeIfAbsent(ruleId, this::listResourceIdsForRule);
            for (UUID resourceId : resourceIds) {
                availabilitySlotResourceRepository.save(
                        new AvailabilitySlotResource(savedSlots.get(i).getId(), resourceId));
            }
        }
    }

    public List<AvailabilitySlot> listOpenSlots(UUID doctorProfileId, Instant from, Instant to) {
        return availabilitySlotRepository.findAllByDoctorProfileIdAndStartsAtBetweenAndBookedFalseOrderByStartsAt(
                doctorProfileId, from, to);
    }

    private DoctorProfile ownDoctorProfileOrThrow(UserContext principal) {
        if (principal.role() != Role.DOCTOR) {
            throw new ForbiddenException("Only doctors can manage availability.");
        }
        return doctorProfileRepository.findByUserId(principal.userId())
                .orElseThrow(() -> new NotFoundException("You don't have a doctor profile yet."));
    }
}
