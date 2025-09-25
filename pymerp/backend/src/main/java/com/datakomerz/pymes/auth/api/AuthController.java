package com.datakomerz.pymes.auth.api;

import com.datakomerz.pymes.auth.application.LoginUseCase;
import com.datakomerz.pymes.auth.application.RefreshTokenUseCase;
import com.datakomerz.pymes.auth.dto.AuthRequest;
import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.auth.dto.RefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final LoginUseCase loginUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;

  public AuthController(LoginUseCase loginUseCase, RefreshTokenUseCase refreshTokenUseCase) {
    this.loginUseCase = loginUseCase;
    this.refreshTokenUseCase = refreshTokenUseCase;
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
}
