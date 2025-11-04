package com.datakomerz.pymes.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateUserAccountRequest(
  @NotBlank @Email String email,
  @NotBlank String name,
  String role,
  List<String> roles,
  String status
) {}
