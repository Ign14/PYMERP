package com.datakomerz.pymes.auth.dto;

import com.datakomerz.pymes.common.captcha.SimpleCaptchaPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthRequest(@NotBlank @Email String email,
                          @NotBlank String password,
                          @NotNull @Valid SimpleCaptchaPayload captcha) {}
