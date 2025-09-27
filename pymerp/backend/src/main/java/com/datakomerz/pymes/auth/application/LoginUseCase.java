package com.datakomerz.pymes.auth.application;

import com.datakomerz.pymes.auth.RefreshTokenService;
import com.datakomerz.pymes.auth.dto.AuthRequest;
import com.datakomerz.pymes.auth.dto.AuthResponse;
import com.datakomerz.pymes.common.captcha.SimpleCaptchaValidationService;
import com.datakomerz.pymes.config.AppProperties;
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
  private final AppProperties appProperties;
  private final RefreshTokenService refreshTokenService;
  private final AuthResponseFactory authResponseFactory;
  private final SimpleCaptchaValidationService captchaValidationService;

  public LoginUseCase(AuthenticationManager authenticationManager,
                      JwtService jwtService,
                      AppProperties appProperties,
                      RefreshTokenService refreshTokenService,
                      AuthResponseFactory authResponseFactory,
                      SimpleCaptchaValidationService captchaValidationService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.appProperties = appProperties;
    this.refreshTokenService = refreshTokenService;
    this.authResponseFactory = authResponseFactory;
    this.captchaValidationService = captchaValidationService;
  }

  public AuthResponse handle(AuthRequest request) {
    captchaValidationService.validate(request.captcha());
    Authentication authentication = authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(request.email(), request.password())
    );

    AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
    String token = jwtService.generateToken(principal);
    long expiresIn = appProperties.getSecurity().getJwt().getExpirationSeconds();
    String refreshToken = refreshTokenService.issueToken(principal.getAccount());
    long refreshExpiresIn = appProperties.getSecurity().getJwt().getRefreshExpirationSeconds();

    return authResponseFactory.build(principal, token, expiresIn, refreshToken, refreshExpiresIn);
  }
}
