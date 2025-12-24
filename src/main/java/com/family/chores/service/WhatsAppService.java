package com.family.chores.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    @Value("${whatsapp.token}")
    private String token;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    private final RestTemplate restTemplate = new RestTemplate();

    // 1. Send Simple Text
    public void sendUserInfoMessage(String toPhoneNumber, String messageText) {
        String url = "https://graph.facebook.com/v17.0/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", toPhoneNumber);
        body.put("type", "text");

        Map<String, String> text = new HashMap<>();
        text.put("body", messageText);
        body.put("text", text);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Message sent to: " + toPhoneNumber);
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    // 2. Send Interactive Buttons (Menu, Accept/Reject)
    public void sendButtonMessage(String toPhoneNumber, String messageText, 
                                  String btn1Id, String btn1Title, 
                                  String btn2Id, String btn2Title) {
        
        String url = "https://graph.facebook.com/v17.0/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", toPhoneNumber);
        body.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "button");

        Map<String, String> bodyText = new HashMap<>();
        bodyText.put("text", messageText);
        interactive.put("body", bodyText);

        Map<String, Object> action = new HashMap<>();
        java.util.List<Map<String, Object>> buttons = new java.util.ArrayList<>();

        // Button 1
        if (btn1Id != null) {
            Map<String, Object> btn1 = new HashMap<>();
            btn1.put("type", "reply");
            Map<String, String> reply1 = new HashMap<>();
            reply1.put("id", btn1Id);
            reply1.put("title", btn1Title);
            btn1.put("reply", reply1);
            buttons.add(btn1);
        }

        // Button 2
        if (btn2Id != null) {
            Map<String, Object> btn2 = new HashMap<>();
            btn2.put("type", "reply");
            Map<String, String> reply2 = new HashMap<>();
            reply2.put("id", btn2Id);
            reply2.put("title", btn2Title);
            btn2.put("reply", reply2);
            buttons.add(btn2);
        }

        action.put("buttons", buttons);
        interactive.put("action", action);
        body.put("interactive", interactive);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Button message sent to: " + toPhoneNumber);
        } catch (Exception e) {
            System.err.println("Failed to send buttons: " + e.getMessage());
        }
    }

    // 3. Forward an Image (The Missing Method!)
    public void sendImageMessage(String toPhoneNumber, String imageId, String caption) {
        String url = "https://graph.facebook.com/v17.0/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", toPhoneNumber);
        body.put("type", "image");

        Map<String, String> imageObject = new HashMap<>();
        imageObject.put("id", imageId);
        imageObject.put("caption", caption);
        
        body.put("image", imageObject);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Image forwarded to: " + toPhoneNumber);
        } catch (Exception e) {
            System.err.println("Failed to send image: " + e.getMessage());
        }
    }
}