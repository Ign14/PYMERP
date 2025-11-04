package com.datakomerz.pymes.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateUserAccountRequest(
  @NotBlank @Email String email,
  @NotBlank String name,
  String role,
  List<String> roles,
  @NotBlank @Size(min = 8, max = 72) String password,
  String status
) {}
