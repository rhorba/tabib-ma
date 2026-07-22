package com.tabibma.identity;

import com.tabibma.identity.dto.AuthResponse;
import com.tabibma.identity.dto.LoginRequest;
import com.tabibma.identity.dto.RefreshRequest;
import com.tabibma.identity.dto.RegisterRequest;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.UnauthorizedException;
import com.tabibma.shared.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Service
public class AuthService {

    /**
     * Self-registration is intentionally limited to Patient/Doctor. Clinic Admin and Platform Admin
     * accounts are created through an internal-only path (clinic invitation / platform seeding) —
     * accepting an arbitrary role from a public registration payload would be a privilege-escalation
     * hole (see Test Strategy adversarial checklist, Auth section).
     */
    private static final Set<Role> SELF_REGISTERABLE_ROLES = Set.of(Role.PATIENT, Role.DOCTOR);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenSecurityService refreshTokenSecurityService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenExpirationMs;

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        RefreshTokenSecurityService refreshTokenSecurityService,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider,
                        @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenSecurityService = refreshTokenSecurityService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (!SELF_REGISTERABLE_ROLES.contains(request.role())) {
            throw new ValidationException("Self-registration is only available for Patient and Doctor roles.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists.");
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()),
                request.role(), request.firstName(), request.lastName());
        return userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        return issueTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String presentedHash = hash(request.refreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHash(presentedHash).orElse(null);

        if (token == null) {
            throw new UnauthorizedException("Invalid refresh token.");
        }
        if (token.getRevokedAt() != null) {
            // Replay of an already-rotated token — treat as theft and revoke every active session (Story 1.3).
            // Committed via its own REQUIRES_NEW transaction — see RefreshTokenSecurityService javadoc for why.
            refreshTokenSecurityService.revokeAllActiveSessions(token.getUserId());
            throw new UnauthorizedException("Refresh token invalid; all sessions have been revoked.");
        }
        if (!token.isValid(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired.");
        }

        token.revoke();
        refreshTokenRepository.save(token);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));
        return issueTokenPair(user);
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String rawRefreshToken = generateRawRefreshToken();
        RefreshToken refreshToken = new RefreshToken(user.getId(), hash(rawRefreshToken),
                Instant.now().plus(Duration.ofMillis(refreshTokenExpirationMs)));
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(accessToken, rawRefreshToken, jwtTokenProvider.getAccessTokenExpirationMs());
    }

    private String generateRawRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
