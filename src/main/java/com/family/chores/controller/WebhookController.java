package com.family.chores.controller;

import com.family.chores.model.FamilyMember;
import com.family.chores.service.ChoreManager;
import com.family.chores.service.WhatsAppService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChoreManager choreManager;
    private final WhatsAppService whatsAppService;

    public WebhookController(ChoreManager choreManager, WhatsAppService whatsAppService) {
        this.choreManager = choreManager;
        this.whatsAppService = whatsAppService;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return new ResponseEntity<>(challenge, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode messages = root.path("entry").get(0)
                                    .path("changes").get(0)
                                    .path("value").path("messages");

            if (!messages.isMissingNode() && messages.isArray()) {
                JsonNode message = messages.get(0);
                String fromRaw = message.path("from").asText();
                String incomingNumber = "+" + fromRaw; 
                String type = message.path("type").asText();

                System.out.println("Received " + type + " from " + incomingNumber);

                if ("image".equals(type)) {
                    FamilyMember guiltyMember = choreManager.getMemberToBlame();
                    String imageId = message.path("image").path("id").asText();
                    whatsAppService.sendImageMessage(guiltyMember.getPhoneNumber(), imageId, 
                        "COMPLAINT: Unfinished work found by " + incomingNumber);
                    whatsAppService.sendUserInfoMessage(incomingNumber, 
                        "Complaint forwarded to " + guiltyMember.getName());
                }
                else if ("interactive".equals(type)) {
                    String buttonId = message.path("interactive").path("button_reply").path("id").asText();
                    
                    if (buttonId.equals("BTN_FORCE_SKIP")) {
                         if (!isAdmin(incomingNumber)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                         choreManager.rotateTurn();
                         whatsAppService.sendUserInfoMessage(incomingNumber, 
                             "Force Skip Successful. New Turn: " + choreManager.getCurrentMember().getName());
                    }
                    else if (buttonId.equals("BTN_RESEND_ALERT")) {
                        if (!isAdmin(incomingNumber)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                        FamilyMember current = choreManager.getCurrentMember();
                        whatsAppService.sendTemplateMessage(current.getPhoneNumber(), "daily_chore_alert", current.getName());
                        whatsAppService.sendUserInfoMessage(incomingNumber, "Alert resent.");
                    }
                    // --- NEW: Handle Schedule Button ---
                    else if (buttonId.equals("BTN_SCHEDULE")) {
                        sendSchedule(incomingNumber);
                    }
                    // -----------------------------------
                    else if (buttonId.equals("BTN_SKIP")) {
                        handleSkipRequest(incomingNumber);
                    } 
                    else if (buttonId.equals("BTN_COMPLAIN")) {
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "I am listening. Please take a PHOTO of the mess (or text me the issue).");
                    }
                    else if (buttonId.equals("BTN_STATUS")) {
                        FamilyMember current = choreManager.getCurrentMember();
                        whatsAppService.sendUserInfoMessage(incomingNumber, "It is " + current.getName() + "'s turn.");
                    } 
                    else if (buttonId.equals("BTN_APPROVE")) {
                        choreManager.approveBypass();
                        whatsAppService.sendUserInfoMessage(incomingNumber, "Request Approved. Turn rotated.");
                    }
                    else if (buttonId.equals("BTN_DENY")) {
                        whatsAppService.sendUserInfoMessage(incomingNumber, "You denied the request.");
                    }
                }
                else if ("text".equals(type)) {
                    String text = message.path("text").path("body").asText().trim();
                    
                    if (text.equalsIgnoreCase("Admin")) {
                        if (isAdmin(incomingNumber)) {
                            // Passing null for 3rd button
                            whatsAppService.sendButtonMessage(incomingNumber, "ADMIN CONTROLS:", 
                                "BTN_FORCE_SKIP", "Force Rotate", 
                                "BTN_RESEND_ALERT", "Resend Alert", null, null);
                        } else {
                            whatsAppService.sendUserInfoMessage(incomingNumber, "⛔ Access Denied.");
                        }
                    }
                    else if (text.equalsIgnoreCase("Menu")) {
                         // --- UPDATED: Now shows 3 buttons ---
                         whatsAppService.sendButtonMessage(incomingNumber, 
                             "What would you like to do?", 
                             "BTN_STATUS", "Status", 
                             "BTN_SCHEDULE", "Schedule", 
                             "BTN_SKIP", "Request Skip");
                    }
                    else if (text.equalsIgnoreCase("Status")) {
                        FamilyMember current = choreManager.getCurrentMember();
                        whatsAppService.sendUserInfoMessage(incomingNumber, "It is " + current.getName() + "'s turn.");
                    }
                    // --- NEW: Handle Text Command ---
                    else if (text.equalsIgnoreCase("Schedule") || text.equalsIgnoreCase("Check Schedule")) {
                        sendSchedule(incomingNumber);
                    }
                    // --------------------------------
                    else {
                        FamilyMember guiltyMember = choreManager.getMemberToBlame();
                        whatsAppService.sendUserInfoMessage(guiltyMember.getPhoneNumber(), 
                            "COMPLAINT from " + incomingNumber + ": " + text);
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "Message forwarded to " + guiltyMember.getName());
                    }
                } 
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
        return new ResponseEntity<>("EVENT_RECEIVED", HttpStatus.OK);
    }

    // --- Helper to build the arrow list ---
    private void sendSchedule(String phoneNumber) {
        StringBuilder sb = new StringBuilder("📅 *Chore Schedule:*\n\n");
        List<FamilyMember> members = choreManager.getFamilyMembers();
        FamilyMember current = choreManager.getCurrentMember();
        
        for (FamilyMember m : members) {
             if (m == current) {
                 sb.append("👉 *").append(m.getName()).append("* (Current)\n");
             } else {
                 sb.append("   ").append(m.getName()).append("\n");
             }
        }
        whatsAppService.sendUserInfoMessage(phoneNumber, sb.toString());
    }

    private boolean isAdmin(String phoneNumber) {
        FamilyMember admin = choreManager.getAdmin();
        return admin.getPhoneNumber().equals(phoneNumber);
    }

    private void handleSkipRequest(String phoneNumber) {
        String result = choreManager.requestBypass(phoneNumber, "Button Request");
        if (result.equals("PENDING_APPROVAL")) {
            whatsAppService.sendUserInfoMessage(phoneNumber, "Request sent to Admin.");
            FamilyMember admin = choreManager.getAdmin();
            // Passing null for 3rd button
            whatsAppService.sendButtonMessage(admin.getPhoneNumber(), 
                "User " + phoneNumber + " requested a skip.", 
                "BTN_APPROVE", "Approve", 
                "BTN_DENY", "Deny", null, null);
        } else if (result.equals("APPROVED_AUTO")) {
            whatsAppService.sendUserInfoMessage(phoneNumber, "Auto-Approved! Turn rotated.");
        } else {
            whatsAppService.sendUserInfoMessage(phoneNumber, result);
        }
    }
}