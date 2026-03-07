package com.example.app.service;

import org.springframework.core.io.ByteArrayResource;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
     void sendAttachMessage(String to, String subject, String text, String attachement);
    void sendAttachMessage(String to, String cc, String subject, String text, String fileName, ByteArrayResource byteArrayResource);
}
