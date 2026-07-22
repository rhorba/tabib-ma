package com.tabibma.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Isolated in its own bean/transaction so the "revoke everything" side effect commits even though
 * the calling AuthService.refresh() transaction goes on to throw and roll back — a replay-detected
 * revocation must not be undone just because the response to the attacker is a 401 (Story 1.3).
 */
@Service
public class RefreshTokenSecurityService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenSecurityService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllActiveSessions(UUID userId) {
        List<RefreshToken> active = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        active.forEach(RefreshToken::revoke);
        refreshTokenRepository.saveAll(active);
    }
}
