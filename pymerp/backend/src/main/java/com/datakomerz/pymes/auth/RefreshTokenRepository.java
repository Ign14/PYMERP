package com.datakomerz.pymes.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
  Optional<RefreshToken> findByIdAndUserId(UUID id, UUID userId);
  void deleteByUserId(UUID userId);
}
