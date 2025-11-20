package com.datakomerz.pymes.config;

import com.datakomerz.pymes.PymesApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
  classes = PymesApplication.class,
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  properties = "app.security.secrets-validation.enabled=false"
)
@ActiveProfiles("test")
class SecretsValidatorStartupTest {

  @Test
  void applicationFailsToStartWhenValidationEnabledAndSecretsMissing() {
    assertThrows(IllegalStateException.class, () -> new SpringApplicationBuilder(PymesApplication.class)
      .web(WebApplicationType.NONE)
      .profiles("test")
      .properties(
        "app.security.secrets-validation.enabled=true"
      )
      .run()
    );
  }
}
