package com.datakomerz.pymes.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

  @Mock
  private AuditLogRepository auditLogRepository;

  @InjectMocks
  private AuditService auditService;

  private AuditLog sampleLog;
  private Long companyId = 1L;

  @BeforeEach
  void setUp() {
    sampleLog = new AuditLog();
    sampleLog.setId(1L);
    sampleLog.setUsername("admin");
    sampleLog.setAction("DELETE");
    sampleLog.setEntityType("Customer");
    sampleLog.setEntityId(123L);
    sampleLog.setCompanyId(companyId);
    sampleLog.setStatusCode(200);
  }

  @Test
  void logAction_ShouldSaveAuditLog() {
    // When
    auditService.logAction(sampleLog);

    // Then
    verify(auditLogRepository, timeout(1000)).save(sampleLog);
  }

  @Test
  void getAuditLogs_ShouldReturnPagedResults() {
    // Given
    Pageable pageable = PageRequest.of(0, 20);
    Page<AuditLog> expectedPage = new PageImpl<>(List.of(sampleLog));
    when(auditLogRepository.findByCompanyIdOrderByTimestampDesc(companyId, pageable))
        .thenReturn(expectedPage);

    // When
    Page<AuditLog> result = auditService.getAuditLogs(companyId, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getUsername()).isEqualTo("admin");
    verify(auditLogRepository).findByCompanyIdOrderByTimestampDesc(companyId, pageable);
  }

  @Test
  void getAuditLogsByUser_ShouldFilterByUsername() {
    // Given
    String username = "admin";
    Pageable pageable = PageRequest.of(0, 20);
    Page<AuditLog> expectedPage = new PageImpl<>(List.of(sampleLog));
    when(auditLogRepository.findByUsernameAndCompanyIdOrderByTimestampDesc(username, companyId, pageable))
        .thenReturn(expectedPage);

    // When
    Page<AuditLog> result = auditService.getAuditLogsByUser(username, companyId, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getUsername()).isEqualTo(username);
    verify(auditLogRepository).findByUsernameAndCompanyIdOrderByTimestampDesc(username, companyId, pageable);
  }

  @Test
  void getAuditLogsByAction_ShouldFilterByAction() {
    // Given
    String action = "DELETE";
    Pageable pageable = PageRequest.of(0, 20);
    Page<AuditLog> expectedPage = new PageImpl<>(List.of(sampleLog));
    when(auditLogRepository.findByActionAndCompanyIdOrderByTimestampDesc(action, companyId, pageable))
        .thenReturn(expectedPage);

    // When
    Page<AuditLog> result = auditService.getAuditLogsByAction(action, companyId, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getAction()).isEqualTo(action);
    verify(auditLogRepository).findByActionAndCompanyIdOrderByTimestampDesc(action, companyId, pageable);
  }

  @Test
  void getFailedRequests_ShouldFilterByStatusCode() {
    // Given
    sampleLog.setStatusCode(403);
    Pageable pageable = PageRequest.of(0, 20);
    Page<AuditLog> expectedPage = new PageImpl<>(List.of(sampleLog));
    when(auditLogRepository.findByStatusCodeGreaterThanEqualAndCompanyId(400, companyId, pageable))
        .thenReturn(expectedPage);

    // When
    Page<AuditLog> result = auditService.getFailedRequests(companyId, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getStatusCode()).isGreaterThanOrEqualTo(400);
    verify(auditLogRepository).findByStatusCodeGreaterThanEqualAndCompanyId(400, companyId, pageable);
  }

  @Test
  void countFailedAccessAttempts_ShouldReturnCount() {
    // Given
    String username = "hacker";
    int hoursAgo = 24;
    when(auditLogRepository.countFailedAccessAttempts(eq(username), any(Instant.class)))
        .thenReturn(5L);

    // When
    long count = auditService.countFailedAccessAttempts(username, hoursAgo);

    // Then
    assertThat(count).isEqualTo(5L);
    verify(auditLogRepository).countFailedAccessAttempts(eq(username), any(Instant.class));
  }
}
