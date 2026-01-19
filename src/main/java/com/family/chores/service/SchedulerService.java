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

    // Currently set to run every minute for testing. 
    @Scheduled(cron = "0 0 8 * * *") 
    public void sendDailyChoreReminder() {
        System.out.println("--- SCHEDULER TRIGGERED: " + LocalDateTime.now() + " ---");

        // 1. Rotate the turn automatically
        choreManager.rotateTurn();

        // 2. Get the new person
        FamilyMember newMember = choreManager.getCurrentMember();
        System.out.println("New Chore Assignee: " + newMember.getName());

        // 3. Send the TEMPLATE
        // This will now work even if the phone hasn't messaged the bot in 24 hours
        whatsAppService.sendTemplateMessage(newMember.getPhoneNumber(), "daily_chore_alert", newMember.getName());
    }

    @Scheduled(cron = "0 0 20 * * *") 
    public void sendDailyChoreReminder2() {
        System.out.println("--- SCHEDULER TRIGGERED: " + LocalDateTime.now() + " ---");

        
        

        // 2. Get the new person
        FamilyMember newMember = choreManager.getCurrentMember();
        

        // 3. Send the TEMPLATE
        // This will now work even if the phone hasn't messaged the bot in 24 hours
        whatsAppService.sendUserInfoMessage(newMember.getPhoneNumber(), "🌞 *Good Evening " + newMember.getName() + "!* \n\nThis is your friendly reminder: *It's your turn!* 🏠✨\n(Please don't make me report you to the Admin...)");
    }

    
}