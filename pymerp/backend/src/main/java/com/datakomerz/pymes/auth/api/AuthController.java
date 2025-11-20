package com.datakomerz.pymes.auth.api;

import com.datakomerz.pymes.auth.application.LoginUseCase;
import com.datakomerz.pymes.auth.application.RefreshTokenUseCase;
import com.datakomerz.pymes.auth.dto.AuthRequest;
import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.auth.dto.RefreshRequest;
import com.datakomerz.pymes.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final LoginUseCase loginUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;
  private final SecurityProperties securityProperties;

  public AuthController(LoginUseCase loginUseCase,
                        RefreshTokenUseCase refreshTokenUseCase,
                        SecurityProperties securityProperties) {
    this.loginUseCase = loginUseCase;
    this.refreshTokenUseCase = refreshTokenUseCase;
    this.securityProperties = securityProperties;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
    return ResponseEntity.ok(loginUseCase.handle(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(HttpServletRequest servletRequest,
                                              @RequestBody(required = false) RefreshRequest requestBody) {
    return ResponseEntity.ok(refreshTokenUseCase.handle(servletRequest, requestBody));
  }

  @GetMapping("/config")
  public ResponseEntity<Map<String, Object>> config() {
    return ResponseEntity.ok(Map.of(
      "captchaEnabled", securityProperties.getCaptcha().isEnabled(),
      "minPasswordLength", 8,
      "requireEmailVerification", false
    ));
  }
}
