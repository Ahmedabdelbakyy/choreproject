package com.family.chores.controller;

import com.family.chores.model.FamilyMember;
import com.family.chores.service.ChoreManager;
import com.family.chores.service.WhatsAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final WhatsAppService whatsAppService;
    private final ChoreManager choreManager;

    public TestController(WhatsAppService whatsAppService, ChoreManager choreManager) {
        this.whatsAppService = whatsAppService;
        this.choreManager = choreManager;
    }

    @GetMapping("/test-message")
    public String triggerTestMessage() {
        // 1. Get your number (The Admin)
        FamilyMember admin = choreManager.getAdmin();
        
        // 2. Send the message
        whatsAppService.sendUserInfoMessage(
            admin.getPhoneNumber(), 
            "Hello from Java! The system is working."
        );
        
        return "Check your WhatsApp! Message sent to " + admin.getPhoneNumber();
    }
}