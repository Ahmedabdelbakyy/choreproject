package com.family.chores.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppService {

    @Value("${whatsapp.token}")
    private String token;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendUserInfoMessage(String toPhoneNumber, String messageText) {
        // ... (Keep existing text method same as before) ...
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

    // --- UPDATED: Now supports up to 3 buttons ---
    public void sendButtonMessage(String toPhoneNumber, String messageText, 
                                  String btn1Id, String btn1Title, 
                                  String btn2Id, String btn2Title,
                                  String btn3Id, String btn3Title) { // <--- Added 3rd button args
        
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
        List<Map<String, Object>> buttons = new ArrayList<>();

        // Helper to add buttons
        addButton(buttons, btn1Id, btn1Title);
        addButton(buttons, btn2Id, btn2Title);
        addButton(buttons, btn3Id, btn3Title); // <--- Add 3rd button

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

    private void addButton(List<Map<String, Object>> buttons, String id, String title) {
        if (id != null && title != null) {
            Map<String, Object> btn = new HashMap<>();
            btn.put("type", "reply");
            Map<String, String> reply = new HashMap<>();
            reply.put("id", id);
            reply.put("title", title);
            btn.put("reply", reply);
            buttons.add(btn);
        }
    }

    // ... (Keep sendImageMessage and sendTemplateMessage same as before) ...
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

    public void sendTemplateMessage(String toPhoneNumber, String templateName, String variable) {
        String url = "https://graph.facebook.com/v17.0/" + phoneNumberId + "/messages";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", toPhoneNumber);
        body.put("type", "template");
        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        Map<String, String> language = new HashMap<>();
        language.put("code", "en"); // Ensure this matches your template!
        template.put("language", language);
        if (variable != null) {
            List<Map<String, Object>> components = new ArrayList<>();
            Map<String, Object> bodyComponent = new HashMap<>();
            bodyComponent.put("type", "body");
            List<Map<String, Object>> parameters = new ArrayList<>();
            Map<String, Object> param = new HashMap<>();
            param.put("type", "text");
            param.put("text", variable);
            parameters.add(param);
            bodyComponent.put("parameters", parameters);
            components.add(bodyComponent);
            template.put("components", components);
        }
        body.put("template", template);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Template sent to: " + toPhoneNumber);
        } catch (Exception e) {
            System.err.println("Failed to send template: " + e.getMessage());
        }
    }
}