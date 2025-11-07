package com.datakomerz.pymes.testsupport;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class AuthTestUtils {
  private AuthTestUtils() {}

  public static RequestPostProcessor admin() {
    return SecurityMockMvcRequestPostProcessors.user("admin@test").roles("ADMIN");
  }

  public static RequestPostProcessor erpUser() {
    return SecurityMockMvcRequestPostProcessors.user("erp@test").roles("ERP_USER");
  }

  public static RequestPostProcessor readonly() {
    return SecurityMockMvcRequestPostProcessors.user("readonly@test").roles("READONLY");
  }

  public static RequestPostProcessor settings() {
    return SecurityMockMvcRequestPostProcessors.user("settings@test").roles("SETTINGS");
  }
}

