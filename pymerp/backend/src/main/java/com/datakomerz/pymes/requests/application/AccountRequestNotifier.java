package com.datakomerz.pymes.requests.application;

import com.datakomerz.pymes.requests.AccountRequest;
import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class AccountRequestNotifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccountRequestNotifier.class);
  private static final String RECIPIENT = "ignacio@datakomerz.com";
  private static final String SUBJECT = "Nueva solicitud de cuenta PYMERP";
  private static final String FROM = "no-reply@pymerp.local";

  private final JavaMailSender mailSender;

  public AccountRequestNotifier(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void notifyNewRequest(AccountRequest request) {
    try {
      var message = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setTo(RECIPIENT);
      helper.setFrom(FROM);
      helper.setSubject(SUBJECT);
      helper.setText(buildBody(request));
      mailSender.send(message);
      LOGGER.info("Notification email sent for account request {}", request.getId());
    } catch (MessagingException ex) {
      LOGGER.warn("Unable to prepare account request email: {}", ex.getMessage());
      throw new MailPreparationException("Failed to prepare account request email", ex);
    } catch (MailException ex) {
      LOGGER.warn("Unable to send account request notification: {}", ex.getMessage());
      throw ex;
    }
  }

  private String buildBody(AccountRequest request) {
    return new StringBuilder()
      .append("Se ha recibido una nueva solicitud de registro o recuperación de cuenta.\n\n")
      .append("RUT: ").append(request.getRut()).append('\n')
      .append("Nombre completo: ").append(request.getFullName()).append('\n')
      .append("Dirección: ").append(request.getAddress()).append('\n')
      .append("Email: ").append(request.getEmail()).append('\n')
      .append("Empresa: ").append(request.getCompanyName()).append('\n')
      .append("ID de solicitud: ").append(request.getId()).append('\n')
      .append("Fecha: ").append(request.getCreatedAt()).append('\n')
      .toString();
  }
}
