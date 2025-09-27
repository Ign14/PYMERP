package com.datakomerz.pymes.common.captcha;

import com.datakomerz.pymes.config.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class SimpleCaptchaValidationService {

  private static final String ERROR_TYPE = "https://pymerp.cl/problems/captcha-invalid";
  private static final String ERROR_FIELD = "captcha.answer";
  private final AppProperties appProperties;

  public SimpleCaptchaValidationService(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  public void validate(SimpleCaptchaPayload payload) {
    if (!appProperties.getSecurity().getCaptcha().isEnabled()) {
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

    AppProperties.Security.Captcha captchaProps = appProperties.getSecurity().getCaptcha();
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
