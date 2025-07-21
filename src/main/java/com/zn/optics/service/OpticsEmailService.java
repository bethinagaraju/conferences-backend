package com.zn.optics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j 
public class OpticsEmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendContactMessage(String name, String email, String subject, String messageBody) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("secretary@globallopmeet.com"); // Correct recipient for Optics
            message.setSubject("Contact Us Form: " + subject);
            message.setText(
                "Name: " + name + "\n" +
                "Email: " + email + "\n\n" +
                "Message:\n" + messageBody
            );
            message.setFrom("secretary@globalrenewablemeet.com"); // Must match SMTP username
            message.setReplyTo(email); // Allows replying directly to user
            mailSender.send(message);
            log.info("Email sent successfully to {}", message.getTo()[0]);
        } catch (MailException e) {
            log.error("Error sending email: {}", e.getMessage(), e);
        }
    }
}
