package com.family.chores.service;

import com.family.chores.model.FamilyMember;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SchedulerService {

    private final ChoreManager choreManager;
    private final WhatsAppService whatsAppService;

    public SchedulerService(ChoreManager choreManager, WhatsAppService whatsAppService) {
        this.choreManager = choreManager;
        this.whatsAppService = whatsAppService;
    }

    // "cron" is a time setting. 
    // "0 * * * * *" means "Run at the start of EVERY minute" (for testing)
    // Later we will change it to "0 0 8 * * *" (8:00 AM daily)
    @Scheduled(cron = "0 * * * * *") 
    public void sendDailyChoreReminder() {
        System.out.println("--- SCHEDULER TRIGGERED: " + LocalDateTime.now() + " ---");

        // 1. Rotate the turn automatically
        choreManager.rotateTurn();

        // 2. Get the new person
        FamilyMember newMember = choreManager.getCurrentMember();
        System.out.println("New Chore Assignee: " + newMember.getName());

        // 3. Send the notification
        // Note: This might fail if the 24-hour window is closed (we will fix this next with Templates)
        whatsAppService.sendUserInfoMessage(newMember.getPhoneNumber(), 
            "GOOD MORNING! It is now your turn to do the chores.");
    }
}