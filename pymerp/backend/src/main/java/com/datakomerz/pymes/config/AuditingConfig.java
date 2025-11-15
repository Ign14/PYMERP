package com.datakomerz.pymes.config;

import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "utcDateTimeProvider")
public class AuditingConfig {

  @Bean
  public AuditorAware<String> auditorProvider() {
    return new AuditorAwareImpl();
  }

  @Bean
  public DateTimeProvider utcDateTimeProvider() {
    return () -> Optional.of(Instant.now());
  }

  static class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public @NonNull Optional<String> getCurrentAuditor() {
      return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
          .filter(Authentication::isAuthenticated)
          .map(Authentication::getName)
          .filter(name -> !"anonymousUser".equals(name));
    }
  }
}
