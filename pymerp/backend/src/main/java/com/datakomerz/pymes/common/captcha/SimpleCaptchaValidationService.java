package com.datakomerz.pymes.common.captcha;

import com.datakomerz.pymes.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimpleCaptchaValidationService {

  private static final Logger LOG = LoggerFactory.getLogger(SimpleCaptchaValidationService.class);
  private static final String ERROR_TYPE = "https://pymerp.cl/problems/captcha-invalid";
  private static final String ERROR_FIELD = "captcha.answer";
  private final SecurityProperties securityProperties;

  public SimpleCaptchaValidationService(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  public void validate(SimpleCaptchaPayload payload) {
    if (!securityProperties.getCaptcha().isEnabled()) {
      LOG.debug("Captcha validation skipped (disabled in configuration)");
      return;
    }
    if (payload == null) {
      throw new CaptchaValidationException("Debes resolver el captcha", ERROR_TYPE, ERROR_FIELD);
    }
    Integer a = payload.a();
    Integer b = payload.b();
    String answer = payload.answer();

    if (a == null || b == null) {
      throw new CaptchaValidationException("Captcha incompleto", ERROR_TYPE, ERROR_FIELD);
    }

    SecurityProperties.Captcha captchaProps = securityProperties.getCaptcha();
    if (a < captchaProps.getMinOperand() || a > captchaProps.getMaxOperand()
      || b < captchaProps.getMinOperand() || b > captchaProps.getMaxOperand()) {
      throw new CaptchaValidationException("Captcha inválido", ERROR_TYPE, ERROR_FIELD);
    }

    if (answer == null || answer.isBlank()) {
      throw new CaptchaValidationException("Debes responder el captcha", ERROR_TYPE, ERROR_FIELD);
    }

    int expected = a + b;
    int provided;
    try {
      provided = Integer.parseInt(answer.trim());
    } catch (NumberFormatException ex) {
      throw new CaptchaValidationException("La respuesta del captcha debe ser numérica", ERROR_TYPE, ERROR_FIELD);
    }

    if (provided != expected) {
      throw new CaptchaValidationException("Captcha inválido", ERROR_TYPE, ERROR_FIELD);
    }
  }
}
