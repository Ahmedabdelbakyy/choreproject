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

    // 1. VERIFICATION
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

    // 2. EVENT LISTENER
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

                // --- 1. HANDLE IMAGES (COMPLAINT SYSTEM) ---
                if ("image".equals(type)) {
                    // Logic: If someone sends a photo, they are complaining about the PREVIOUS person
                    FamilyMember guiltyMember = choreManager.getMemberToBlame();
                    String imageId = message.path("image").path("id").asText();
                    
                    // A. Forward the photo to the guilty person
                    whatsAppService.sendImageMessage(guiltyMember.getPhoneNumber(), imageId, 
                        "COMPLAINT: Unfinished work found by " + incomingNumber);
                    
                    // B. Confirm to the sender
                    whatsAppService.sendUserInfoMessage(incomingNumber, 
                        "Complaint forwarded to " + guiltyMember.getName());
                }

                // --- 2. HANDLE BUTTON CLICKS ---
                else if ("interactive".equals(type)) {
                    String buttonId = message.path("interactive").path("button_reply").path("id").asText();
                    System.out.println("Button Clicked: " + buttonId);

                    if (buttonId.equals("BTN_SKIP")) {
                        handleSkipRequest(incomingNumber);
                    } 
                    else if (buttonId.equals("BTN_STATUS")) {
                        FamilyMember current = choreManager.getCurrentMember();
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "It is currently " + current.getName() + "'s turn.");
                    } 
                    else if (buttonId.equals("BTN_APPROVE")) {
                        // Admin clicked Approve for a skip request
                        choreManager.approveBypass();
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "Request Approved. The turn has been rotated.");
                    }
                    else if (buttonId.equals("BTN_DENY")) {
                        whatsAppService.sendUserInfoMessage(incomingNumber, "You denied the request.");
                    }
                }

                // --- 3. HANDLE TEXT MESSAGES ---
                else if ("text".equals(type)) {
                    String text = message.path("text").path("body").asText().trim();
                    
                    // A. COMMANDS (Menu / Status)
                    if (text.equalsIgnoreCase("Menu")) {
                         whatsAppService.sendButtonMessage(incomingNumber, 
                             "What would you like to do?", 
                             "BTN_STATUS", "Check Status", 
                             "BTN_SKIP", "Request Skip");
                    }
                    else if (text.equalsIgnoreCase("Status")) {
                        FamilyMember current = choreManager.getCurrentMember();
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "It is currently " + current.getName() + "'s turn.");
                    }
                    
                    // B. TEXT COMPLAINT (If not a command)
                    else {
                        // Treat random text as a complaint description
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

    private void handleSkipRequest(String phoneNumber) {
        String result = choreManager.requestBypass(phoneNumber, "Button Request");
        
        if (result.equals("PENDING_APPROVAL")) {
            whatsAppService.sendUserInfoMessage(phoneNumber, "Request sent to Admin for approval.");
            
            // Send Interactive Buttons to Admin
            FamilyMember admin = choreManager.getAdmin();
            whatsAppService.sendButtonMessage(admin.getPhoneNumber(), 
                "User " + phoneNumber + " requested a skip.", 
                "BTN_APPROVE", "Approve", 
                "BTN_DENY", "Deny");
        } 
        else if (result.equals("APPROVED_AUTO")) {
            whatsAppService.sendUserInfoMessage(phoneNumber, "Auto-Approved! Turn rotated.");
        } 
        else {
            whatsAppService.sendUserInfoMessage(phoneNumber, result);
        }
    }
}