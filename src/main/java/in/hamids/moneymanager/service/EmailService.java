package in.hamids.moneymanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // allow missing env/property during tests by providing an empty default
    @Value("${spring.mail.properties.mail.smtp.from:${BREVO_FROM_EMAIL:}}")
    private String fromEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            String maskedUser = (mailUsername != null && mailUsername.length() > 4) 
                ? mailUsername.substring(0, 4) + "****" 
                : "unknown";
            System.err.println("SMTP Error - User: " + mailUsername + ", Sender: " + fromEmail + ", Error: " + e.getMessage());
            e.printStackTrace(); 
            throw new RuntimeException("Email service failed (Auth User: " + maskedUser + "): " + e.getMessage());
        }
    }
}
