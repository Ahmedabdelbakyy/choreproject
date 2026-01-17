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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChoreManager choreManager;
    private final WhatsAppService whatsAppService;

    private final Map<String, String> userStates = new ConcurrentHashMap<>();

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

                // --- 1. HANDLE IMAGES ---
                if ("image".equals(type)) {
                    if (isUserComplaining(incomingNumber)) {
                        FamilyMember guiltyMember = choreManager.getMemberToBlame();
                        String imageId = message.path("image").path("id").asText();
                        
                        whatsAppService.sendImageMessage(guiltyMember.getPhoneNumber(), imageId, 
                            "🚨 *COMPLAINT FILED* 🚨\n\nUnfinished work reported by " + incomingNumber + ".\nCheck the photo! 📸");
                        
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "✅ Complaint forwarded to *" + guiltyMember.getName() + "*.\nHopefully they fix it soon! 🧹");
                        
                        userStates.remove(incomingNumber);
                    } else {
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "❓ *I received your photo, but I'm confused.*\n\nIf this is evidence of a mess, please click *'Complain'* in the menu first! 📸");
                    }
                }

                else if ("button".equals(type)) {
                    String buttonId = message.path("button").path("payload").asText();

                    if (buttonId.equals("Complain")) {
                        userStates.put(incomingNumber, "COMPLAINING");
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "👂 *I am listening.*\n\n📸 Please send a *PHOTO* of the mess (or text me the details) now. I'm ready to snitch! 📝");
                    }
                    else if (buttonId.equals("Bypass")) {
                        handleSkipRequest(incomingNumber);
                    } 
                }

                // --- 2. HANDLE INTERACTIVE BUTTONS ---
                else if ("interactive".equals(type)) {
                    String buttonId = message.path("interactive").path("button_reply").path("id").asText();

                    if (buttonId.equals("BTN_FORCE_SKIP")) {
                         if (!isAdmin(incomingNumber)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                         choreManager.rotateTurn();
                         whatsAppService.sendUserInfoMessage(incomingNumber, 
                             "⚡ *Force Skip Successful!* ⚡\n\nNew Turn: *" + choreManager.getCurrentMember().getName() + "*");
                         
                         // Notify the new person
                         whatsAppService.sendTemplateMessage(choreManager.getCurrentMember().getPhoneNumber(), "daily_chore_alert", choreManager.getCurrentMember().getName());
                    }
                    else if (buttonId.equals("BTN_RESEND_ALERT")) {
                        if (!isAdmin(incomingNumber)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                        FamilyMember current = choreManager.getCurrentMember();
                        whatsAppService.sendTemplateMessage(current.getPhoneNumber(), "daily_chore_alert", current.getName());
                        whatsAppService.sendUserInfoMessage(incomingNumber, "📢 *Alert Resent!* They should have received it now.");
                    }
                    else if (buttonId.equals("BTN_SCHEDULE")) {
                        sendSchedule(incomingNumber);
                    }
                    else if (buttonId.equals("BTN_SKIP")) {
                        handleSkipRequest(incomingNumber);
                    } 
                    else if (buttonId.equals("BTN_STATUS")) {
                        FamilyMember current = choreManager.getCurrentMember();
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "⏳ *Current Status*\n\nIt is currently *" + current.getName() + "'s* turn to work! 🏠");
                    } 
                    else if (buttonId.equals("BTN_APPROVE")) {
                        choreManager.approveBypass();
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "✅ *Request Approved!* \n\nThe turn has been rotated. Peace restored. 🕊️");

                        FamilyMember newMember = choreManager.getCurrentMember();
                        whatsAppService.sendTemplateMessage(newMember.getPhoneNumber(), "daily_chore_alert", newMember.getName());
                    }
                    else if (buttonId.equals("BTN_DENY")) {
                        whatsAppService.sendUserInfoMessage(incomingNumber, "🚫 *Request Denied.* \n\nYou said NO. Tough love! 💪");
                        whatsAppService.sendUserInfoMessage(choreManager.getCurrentMember().getPhoneNumber(), 
                            "❌ *Request Denied*\n\nThe Admin said no. Get back to work! 🧹");
                    }
                }

                // --- 3. HANDLE TEXT MESSAGES ---
                else if ("text".equals(type)) {
                    String text = message.path("text").path("body").asText().trim();
                    
                    if (text.equalsIgnoreCase("Admin")) {
                        if (isAdmin(incomingNumber)) {
                            whatsAppService.sendButtonMessage(incomingNumber, 
                                "🔒 *ADMIN CONTROLS*", 
                                "BTN_FORCE_SKIP", "Force Rotate", 
                                "BTN_RESEND_ALERT", "Resend Alert", null, null);
                        } else {
                            whatsAppService.sendUserInfoMessage(incomingNumber, "⛔ *Access Denied* ⛔\n\nNice try, but you are not the Admin! 🕵️‍♂️");
                        }
                    }
                    else if (text.equalsIgnoreCase("Menu")) {
                         whatsAppService.sendButtonMessage(incomingNumber, 
                             "🤖 *Beep Boop* \n\nWhat would you like to do?", 
                             "BTN_STATUS", "Status", 
                             "BTN_SCHEDULE", "Schedule", 
                             "BTN_SKIP", "Request Skip");
                    }
                    else if (text.equalsIgnoreCase("Status")) {
                        FamilyMember current = choreManager.getCurrentMember();
                        whatsAppService.sendUserInfoMessage(incomingNumber, 
                            "⏳ *Current Status*\n\nIt is currently *" + current.getName() + "'s* turn to work! 🏠");
                    }
                    else if (text.equalsIgnoreCase("Schedule") || text.equalsIgnoreCase("Check Schedule")) {
                        sendSchedule(incomingNumber);
                    }
                    
                    else {
                        if (isUserComplaining(incomingNumber)) {
                            FamilyMember guiltyMember = choreManager.getMemberToBlame();
                            
                            whatsAppService.sendUserInfoMessage(guiltyMember.getPhoneNumber(), 
                                "😤 *COMPLAINT from* " + incomingNumber + ":\n\n\"" + text + "\"");
                                
                            whatsAppService.sendUserInfoMessage(incomingNumber, 
                                "📨 *Message forwarded to* " + guiltyMember.getName() + ". Let's see what they say.");
                                
                            userStates.remove(incomingNumber);
                        } else {
                            whatsAppService.sendButtonMessage(incomingNumber, 
                                "🤖 *Does not compute...*\n\nI didn't understand that. Please use the Menu below: 👇", 
                                "BTN_STATUS", "Status", 
                                "BTN_SCHEDULE", "Schedule", 
                                "BTN_SKIP", "Request Skip");
                        }
                    }
                } 
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }

        return new ResponseEntity<>("EVENT_RECEIVED", HttpStatus.OK);
    }

    private boolean isUserComplaining(String phoneNumber) {
        return "COMPLAINING".equals(userStates.get(phoneNumber));
    }

    private void sendSchedule(String phoneNumber) {
        StringBuilder sb = new StringBuilder("📅 *Chore Schedule:*\n\n");
        List<FamilyMember> members = choreManager.getFamilyMembers();
        FamilyMember current = choreManager.getCurrentMember();
        
        for (FamilyMember m : members) {
             if (m.getPhoneNumber().equals(current.getPhoneNumber())) {
                 sb.append("👉 *").append(m.getName()).append("* (Current) 🏠\n");
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
            whatsAppService.sendUserInfoMessage(phoneNumber, "⏳ *Request Pending*\n\nSent to Admin for approval. Fingers crossed! 🤞");
            
            FamilyMember admin = choreManager.getAdmin();
            whatsAppService.sendButtonMessage(admin.getPhoneNumber(), 
                "📩 *New Request*\n\nUser " + phoneNumber + " wants to skip their turn. \n\n*Approve?*", 
                "BTN_APPROVE", "Approve", 
                "BTN_DENY", "Deny", null, null);
        } 
        else if (result.equals("APPROVED_AUTO")) {
            if(choreManager.getCurrentMember().getName().equalsIgnoreCase("Ashraf")) {
                whatsAppService.sendUserInfoMessage(phoneNumber, "✈️ *Safe Travels!* \n\nAuto-Approved! Have a safe trip Ashraf! 👋");
            }
            else{
                whatsAppService.sendUserInfoMessage(phoneNumber, "✅ *Auto-Approved!* \n\nTurn rotated automatically. Enjoy your break! ☕");
            }
             choreManager.approveBypass();

            FamilyMember newMember = choreManager.getCurrentMember();
            whatsAppService.sendTemplateMessage(newMember.getPhoneNumber(), "daily_chore_alert", newMember.getName());
        } 
        else {
            whatsAppService.sendUserInfoMessage(phoneNumber, result);
        }
    }
}