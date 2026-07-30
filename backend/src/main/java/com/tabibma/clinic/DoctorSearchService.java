package com.tabibma.clinic;

import com.tabibma.clinic.dto.DoctorPublicProfileResponse;
import com.tabibma.clinic.dto.DoctorSearchResponse;
import com.tabibma.clinic.dto.DoctorSearchResultResponse;
import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class DoctorSearchService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

    public DoctorSearchService(DoctorProfileRepository doctorProfileRepository, UserRepository userRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
    }

    @Cacheable(value = "doctorSearch", key = "(#specialty ?: '') + ':' + (#city ?: '') + ':' + #page + ':' + #size")
    public DoctorSearchResponse search(String specialty, String city, int page, int size) {
        Page<DoctorProfile> profiles = doctorProfileRepository.search(
                blankToNull(specialty), blankToNull(city), PageRequest.of(page, size));

        List<UUID> userIds = profiles.getContent().stream().map(DoctorProfile::getUserId).toList();
        Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, Function.identity()));

        List<DoctorSearchResultResponse> results = profiles.getContent().stream()
                .map(profile -> {
                    User user = usersById.get(profile.getUserId());
                    return new DoctorSearchResultResponse(
                            profile.getId(),
                            user != null ? user.getFirstName() : null,
                            user != null ? user.getLastName() : null,
                            profile.getSpecialty(),
                            profile.getCity(),
                            profile.getConsultationFeeMad());
                })
                .toList();

        return new DoctorSearchResponse(results, profiles.getNumber(), profiles.getSize(),
                profiles.getTotalElements(), profiles.getTotalPages());
    }

    public DoctorPublicProfileResponse getPublicProfile(UUID doctorProfileId) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorProfileId)
                .filter(p -> p.getVerificationStatus() == VerificationStatus.APPROVED)
                .orElseThrow(() -> new NotFoundException("Doctor profile not found."));

        User user = userRepository.findById(profile.getUserId()).orElse(null);

        // No review module yet (Epic 9) — degrade gracefully per stories-tabib-ma.md Story 3.2's
        // "reviews — degrade gracefully if none exist yet" note, rather than blocking this story on it.
        return new DoctorPublicProfileResponse(
                profile.getId(),
                user != null ? user.getFirstName() : null,
                user != null ? user.getLastName() : null,
                profile.getSpecialty(),
                profile.getCity(),
                profile.getBio(),
                profile.getConsultationFeeMad(),
                null,
                0L);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
