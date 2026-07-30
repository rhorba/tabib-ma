package com.tabibma.clinic;

import com.tabibma.clinic.dto.DoctorSearchResponse;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorSearchServiceTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private UserRepository userRepository;

    private DoctorSearchService service() {
        return new DoctorSearchService(doctorProfileRepository, userRepository);
    }

    @Test
    void search_blankFiltersAreTreatedAsNoFilter() {
        UUID userId = UUID.randomUUID();
        DoctorProfile profile = new DoctorProfile(userId, "Cardiology", "bio", BigDecimal.valueOf(200), "Rabat");
        Page<DoctorProfile> page = new PageImpl<>(List.of(profile));
        when(doctorProfileRepository.search(isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of());

        DoctorSearchResponse response = service().search("  ", "", 0, 20);

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).specialty()).isEqualTo("Cardiology");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void search_resolvesDoctorNameFromMatchingUser() {
        UUID userId = UUID.randomUUID();
        DoctorProfile profile = new DoctorProfile(userId, "Dermatology", "bio", BigDecimal.valueOf(150), "Fes");
        Page<DoctorProfile> page = new PageImpl<>(List.of(profile));
        when(doctorProfileRepository.search(eq("Dermatology"), eq("Fes"), any(Pageable.class))).thenReturn(page);

        User user = new User("d@example.com", "hash", Role.DOCTOR, "Amina", "Bennani");
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        DoctorSearchResponse response = service().search("Dermatology", "Fes", 0, 20);

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).firstName()).isEqualTo("Amina");
        assertThat(response.results().get(0).lastName()).isEqualTo("Bennani");
    }

    @Test
    void search_missingUserStillReturnsProfileWithNullName() {
        UUID userId = UUID.randomUUID();
        DoctorProfile profile = new DoctorProfile(userId, "Urology", "bio", BigDecimal.valueOf(150), "Oujda");
        Page<DoctorProfile> page = new PageImpl<>(List.of(profile));
        when(doctorProfileRepository.search(eq("Urology"), eq("Oujda"), any(Pageable.class))).thenReturn(page);
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of());

        DoctorSearchResponse response = service().search("Urology", "Oujda", 0, 20);

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).firstName()).isNull();
        assertThat(response.results().get(0).lastName()).isNull();
    }

    @Test
    void search_emptyResultsReturnsEmptyList() {
        Page<DoctorProfile> page = new PageImpl<>(List.of());
        when(doctorProfileRepository.search(eq("Oncology"), isNull(), any(Pageable.class))).thenReturn(page);

        DoctorSearchResponse response = service().search("Oncology", null, 0, 20);

        assertThat(response.results()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }
}
