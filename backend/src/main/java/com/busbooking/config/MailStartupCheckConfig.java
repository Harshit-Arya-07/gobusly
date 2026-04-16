package com.busbooking.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MailStartupCheckConfig {

    private final JavaMailSender mailSender;

    @Value("${app.mail.test-connection-on-startup:true}")
    private boolean testConnectionOnStartup;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:0}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public MailStartupCheckConfig(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void testMailConnection() {
        if (!testConnectionOnStartup) {
            log.info("SMTP startup test is disabled (app.mail.test-connection-on-startup=false)");
            return;
        }

        String safeUser = (mailUsername == null || mailUsername.isBlank()) ? "<empty>" : mailUsername;
        log.info("Testing SMTP connection on startup: host={}, port={}, username={}", mailHost, mailPort, safeUser);

        if (mailSender instanceof JavaMailSenderImpl javaMailSender) {
            try {
                javaMailSender.testConnection();
                log.info("SMTP connection test passed.");
            } catch (Exception ex) {
                log.error("SMTP connection test failed. Check Render env vars and Gmail App Password.", ex);
            }
            return;
        }

        log.warn("SMTP connection test skipped: JavaMailSender implementation does not support testConnection().");
    }
}
