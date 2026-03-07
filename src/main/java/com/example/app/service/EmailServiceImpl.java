package com.example.app.service;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender emailSender;

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    public EmailServiceImpl(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void sendSimpleMessage(String to, String subject, String text){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("m.quaschny@dataport.de");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }


    @Override
    public void sendAttachMessage(String to, String cc, String subject, String text) {
        MimeMessagePreparator preparator = mimeMessage -> {
            // Handle multiple "To" recipients
            if (to != null && !to.isEmpty()) {
                String[] toRecipients = to.split(";");
                InternetAddress[] toAddresses = new InternetAddress[toRecipients.length];
                for (int i = 0; i < toRecipients.length; i++) {
                    toAddresses[i] = new InternetAddress(toRecipients[i].trim());
                }
                mimeMessage.setRecipients(Message.RecipientType.TO, toAddresses);
            }

            // Handle multiple "CC" recipients
            if (cc != null && !cc.isEmpty()) {
                String[] ccRecipients = cc.split(";");
                InternetAddress[] ccAddresses = new InternetAddress[ccRecipients.length];
                for (int i = 0; i < ccRecipients.length; i++) {
                    ccAddresses[i] = new InternetAddress(ccRecipients[i].trim());
                }
                mimeMessage.setRecipients(Message.RecipientType.CC, ccAddresses);
            }
//            if (cc != null && !cc.isEmpty()) {
//                mimeMessage.setRecipient(Message.RecipientType.CC, new InternetAddress(cc));
//            }
//            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setFrom(new InternetAddress("noreplay@dataport.de"));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(text);

            try {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                //   helper.addAttachment(attachmentFileName, byteArrayResource);
                helper.setText(text, true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };
        System.out.println("Email sending.......");
      //  emailSender.send(preparator);
        System.out.println("Email send success.......");

    }

    @Override
    public void sendAttachMessage(String to, String cc, String subject, String text, String attachmentFileName, ByteArrayResource byteArrayResource) {
        try {
            MimeMessagePreparator preparator = mimeMessage -> {
            // Handle multiple "To" recipients
            if (to != null && !to.isEmpty()) {
                String[] toRecipients = to.split(";");
                InternetAddress[] toAddresses = new InternetAddress[toRecipients.length];
                for (int i = 0; i < toRecipients.length; i++) {
                    toAddresses[i] = new InternetAddress(toRecipients[i].trim());
                }
                mimeMessage.setRecipients(Message.RecipientType.TO, toAddresses);
            }

            // Handle multiple "CC" recipients
            if (cc != null && !cc.isEmpty()) {
                String[] ccRecipients = cc.split(";");
                InternetAddress[] ccAddresses = new InternetAddress[ccRecipients.length];
                for (int i = 0; i < ccRecipients.length; i++) {
                    ccAddresses[i] = new InternetAddress(ccRecipients[i].trim());
                }
                mimeMessage.setRecipients(Message.RecipientType.CC, ccAddresses);
            }
//            if (cc != null && !cc.isEmpty()) {
//                mimeMessage.setRecipient(Message.RecipientType.CC, new InternetAddress(cc));
//            }
//            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setFrom(new InternetAddress("noreplay@dataport.de"));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(text);

            try {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.addAttachment(attachmentFileName, byteArrayResource);
                helper.setText(text, true);
            } catch (Exception ex) {
                logger.error("Error occurred while MimeMessageHelper: {}", ex.getMessage());
            //    ex.printStackTrace();
            }
        };

        emailSender.send(preparator);
        } catch (Exception e) {
            logger.error("Error occurred while sending email to {}: {}", to, e.getMessage());
        }
    }


}
