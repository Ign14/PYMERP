package com.datakomerz.pymes.audit;

import com.datakomerz.pymes.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditInterceptorTest {

  @Mock
  private AuditService auditService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HandlerMethod handlerMethod;

  @InjectMocks
  private AuditInterceptor auditInterceptor;

  @BeforeEach
  void setUp() {
    // Mocks se configuran en cada test según sea necesario
  }

  @Test
  void preHandle_ShouldReturnTrueForAuditedEndpoint() throws Exception {
    // Given
    Method method = TestController.class.getMethod("deleteCustomer", Long.class);
    when(handlerMethod.getMethod()).thenReturn(method);

    // When
    boolean result = auditInterceptor.preHandle(request, response, handlerMethod);

    // Then
    assertThat(result).isTrue();
    verify(request).setAttribute(eq("auditStartTime"), any(Instant.class));
    verify(request).setAttribute(eq("auditAnnotation"), any(Audited.class));
  }

  @Test
  void preHandle_ShouldReturnTrueForNonAuditedEndpoint() throws Exception {
    // Given
    Method method = TestController.class.getMethod("getNonAudited");
    when(handlerMethod.getMethod()).thenReturn(method);

    // When
    boolean result = auditInterceptor.preHandle(request, response, handlerMethod);

    // Then
    assertThat(result).isTrue();
    verify(request, never()).setAttribute(anyString(), any());
  }

  @Test
  void afterCompletion_ShouldLogActionForAuditedEndpoint() throws Exception {
    // Given
    when(request.getRequestURI()).thenReturn("/api/v1/customers/123");
    when(request.getMethod()).thenReturn("DELETE");
    when(request.getRemoteAddr()).thenReturn("192.168.1.1");
    when(request.getHeader("X-Forwarded-For")).thenReturn(null); // Mock for getClientIp()
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    Method method = TestController.class.getMethod("deleteCustomer", Long.class);
    Audited audited = method.getAnnotation(Audited.class);

    when(request.getAttribute("auditAnnotation")).thenReturn(audited);
    when(request.getAttribute("auditStartTime")).thenReturn(Instant.now().minusMillis(100));
    when(response.getStatus()).thenReturn(200);

    try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
      securityUtilsMock.when(SecurityUtils::getCurrentUsername).thenReturn(java.util.Optional.of("admin"));
      securityUtilsMock.when(SecurityUtils::getCurrentUserRoles).thenReturn(java.util.List.of("ROLE_ADMIN"));
      securityUtilsMock.when(SecurityUtils::getCurrentUserCompanyId).thenReturn(java.util.Optional.of(1L));

      // When
      auditInterceptor.afterCompletion(request, response, handlerMethod, null);

      // Then
      ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
      verify(auditService, timeout(1000)).logAction(logCaptor.capture());

      AuditLog capturedLog = logCaptor.getValue();
      assertThat(capturedLog.getUsername()).isEqualTo("admin");
      assertThat(capturedLog.getAction()).isEqualTo("DELETE");
      assertThat(capturedLog.getEntityType()).isEqualTo("Customer");
      assertThat(capturedLog.getEntityId()).isEqualTo(123L);
      assertThat(capturedLog.getEndpoint()).isEqualTo("/api/v1/customers/123");
      assertThat(capturedLog.getHttpMethod()).isEqualTo("DELETE");
      assertThat(capturedLog.getCompanyId()).isEqualTo(1L);
      assertThat(capturedLog.getStatusCode()).isEqualTo(200);
      assertThat(capturedLog.getResponseTimeMs()).isGreaterThan(0L);
    }
  }

  // Test controller class
  static class TestController {
    @Audited(action = "DELETE", entityType = "Customer")
    public void deleteCustomer(Long id) {
      // Test method
    }

    public void getNonAudited() {
      // Test method without @Audited
    }
  }
}
