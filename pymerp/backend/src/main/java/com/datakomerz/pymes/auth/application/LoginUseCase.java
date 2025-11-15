package com.datakomerz.pymes.auth.application;

import com.datakomerz.pymes.auth.RefreshTokenService;
import com.datakomerz.pymes.auth.dto.AuthRequest;
import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.config.SecurityProperties;
import com.datakomerz.pymes.security.AppUserDetails;
import com.datakomerz.pymes.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class LoginUseCase {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final SecurityProperties securityProperties;
  private final RefreshTokenService refreshTokenService;
  private final AuthResponseFactory authResponseFactory;

  public LoginUseCase(AuthenticationManager authenticationManager,
                      JwtService jwtService,
                      SecurityProperties securityProperties,
                      RefreshTokenService refreshTokenService,
                      AuthResponseFactory authResponseFactory) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.securityProperties = securityProperties;
    this.refreshTokenService = refreshTokenService;
    this.authResponseFactory = authResponseFactory;
  }

  public AuthResponse handle(AuthRequest request) {
    Authentication authentication = authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(request.email(), request.password())
    );

    AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
    String token = jwtService.generateToken(principal);
    long expiresIn = securityProperties.getJwt().getExpirationSeconds();
    String refreshToken = refreshTokenService.issueToken(principal.getAccount());
    long refreshExpiresIn = securityProperties.getJwt().getRefreshExpirationSeconds();

    return authResponseFactory.build(principal, token, expiresIn, refreshToken, refreshExpiresIn);
  }
}
