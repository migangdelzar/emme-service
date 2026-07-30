package com.emme.notification.provider;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SMTP email provider using Jakarta Mail directly.
 *
 * <p>Credentials read from environment: SMTP_HOST — SMTP server hostname (default: localhost)
 * SMTP_PORT — SMTP port (default: 587) SMTP_USERNAME — SMTP auth username SMTP_PASSWORD — SMTP auth
 * password
 *
 * <p>Falls back to mock when credentials are absent.
 */
@Component
@ConditionalOnProperty(name = "app.notification.email.provider", havingValue = "smtp")
public class SmtpEmailProvider implements EmailProvider {

  private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final boolean configured;

  /** Production constructor — reads credentials from environment. */
  public SmtpEmailProvider() {
    this.host = System.getenv("SMTP_HOST");
    this.username = System.getenv("SMTP_USERNAME");
    this.password = System.getenv("SMTP_PASSWORD");

    String portEnv = System.getenv("SMTP_PORT");
    this.port = portEnv != null && !portEnv.isBlank() ? Integer.parseInt(portEnv) : 587;

    this.configured =
        host != null
            && !host.isBlank()
            && username != null
            && !username.isBlank()
            && password != null
            && !password.isBlank();

    if (!configured) {
      log.warn(
          "SMTP credentials not configured (SMTP_HOST, SMTP_USERNAME, SMTP_PASSWORD). "
              + "Emails will NOT be sent. Set app.notification.email.provider=mock for dev.");
    }
  }

  /** Test constructor — injects all config directly. */
  public SmtpEmailProvider(String host, int port, String username, String password) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.configured = host != null && !host.isBlank();
  }

  @Override
  public String name() {
    return "smtp";
  }

  @Override
  public String send(String to, String subject, String body, String html) {
    if (!configured) {
      throw new EmailProviderException(
          "SMTP not configured: missing SMTP_HOST, SMTP_USERNAME, or SMTP_PASSWORD");
    }

    try {
      Properties props = new Properties();
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.port", String.valueOf(port));
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "true");

      Session session = Session.getInstance(props);
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(username));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
      message.setSubject(subject);

      if (html != null && !html.isBlank()) {
        message.setContent(html, "text/html; charset=utf-8");
      } else {
        message.setText(body, "utf-8");
      }

      String messageId = "<" + UUID.randomUUID() + "@emme>";
      message.setHeader("Message-ID", messageId);

      Transport.send(message, username, password);

      log.info("SMTP email sent: {} -> {} subject='{}'", username, to, subject);
      return messageId;
    } catch (MessagingException e) {
      throw new EmailProviderException("SMTP send failed: " + e.getMessage(), e);
    }
  }
}
