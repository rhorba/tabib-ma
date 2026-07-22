package com.tabibma.identity;

import com.tabibma.identity.dto.LoginRequest;
import com.tabibma.identity.dto.RefreshRequest;
import com.tabibma.identity.dto.RegisterRequest;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.UnauthorizedException;
import com.tabibma.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RefreshTokenSecurityService refreshTokenSecurityService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, refreshTokenSecurityService,
                passwordEncoder, jwtTokenProvider, 2_592_000_000L);
    }

    @Test
    void register_rejectsClinicAdminSelfRegistration() {
        RegisterRequest request = new RegisterRequest("admin@example.com", "password123", Role.CLINIC_ADMIN, "A", "B");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_rejectsPlatformAdminSelfRegistration() {
        RegisterRequest request = new RegisterRequest("admin@example.com", "password123", Role.PLATFORM_ADMIN, "A", "B");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("patient@example.com", "password123", Role.PATIENT, "A", "B");
        when(userRepository.existsByEmail("patient@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void register_allowsPatientAndSavesHashedPassword() {
        RegisterRequest request = new RegisterRequest("patient@example.com", "password123", Role.PATIENT, "A", "B");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(request);

        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.PATIENT);
    }

    @Test
    void login_withUnknownEmail_throwsGenericUnauthorized() {
        when(userRepository.findByEmailAndDeletedAtIsNull("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void login_withWrongPassword_throwsSameGenericMessage() {
        User user = new User("patient@example.com", "hashed", Role.PATIENT, "A", "B");
        when(userRepository.findByEmailAndDeletedAtIsNull("patient@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("patient@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void refresh_withUnknownToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("garbage-token")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_withAlreadyRevokedToken_triggersRevokeAllActiveSessions() {
        UUID userId = UUID.randomUUID();
        RefreshToken revoked = new RefreshToken(userId, "hash", Instant.now().plusSeconds(3600));
        revoked.revoke();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("replayed-token")))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenSecurityService).revokeAllActiveSessions(userId);
    }

    @Test
    void refresh_withExpiredToken_throwsUnauthorized() {
        UUID userId = UUID.randomUUID();
        RefreshToken expired = new RefreshToken(userId, "hash", Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("expired-token")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
