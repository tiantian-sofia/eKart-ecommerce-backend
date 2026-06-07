package com.vedasole.ekartecommercebackend.config;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;

/**
 * Provides a no-op {@link JavaMailSender} for the {@code dev} profile so that
 * flows which send emails (e.g. customer registration) work locally without a
 * real SMTP server. Emails are logged instead of being sent.
 */
@Configuration
@Profile("dev")
@Slf4j
public class DevMailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl() {
            @Override
            public @NonNull MimeMessage createMimeMessage() {
                return new MimeMessage((Session) null);
            }

            @Override
            public @NonNull MimeMessage createMimeMessage(@NonNull InputStream contentStream) {
                return createMimeMessage();
            }

            @Override
            public void send(@NonNull MimeMessage... mimeMessages) {
                log.info("[dev] Skipping real email send ({} message(s)) - mail is mocked in dev profile", mimeMessages.length);
            }

            @Override
            public void send(@NonNull MimeMessagePreparator... mimeMessagePreparators) {
                log.info("[dev] Skipping real email send ({} preparator(s)) - mail is mocked in dev profile", mimeMessagePreparators.length);
            }

            @Override
            public void send(@NonNull SimpleMailMessage... simpleMessages) {
                log.info("[dev] Skipping real email send ({} message(s)) - mail is mocked in dev profile", simpleMessages.length);
            }
        };
    }
}
