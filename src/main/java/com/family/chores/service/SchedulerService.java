package com.family.chores.service;

import com.family.chores.model.FamilyMember;
import com.family.chores.repository.FamilyMemberRepository; // Import this
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SchedulerService {

    private final ChoreManager choreManager;
    private final WhatsAppService whatsAppService;
    private final FamilyMemberRepository repository; // 1. Inject Repository

    // 2. Update Constructor to include Repository
    public SchedulerService(ChoreManager choreManager, WhatsAppService whatsAppService, FamilyMemberRepository repository) {
        this.choreManager = choreManager;
        this.whatsAppService = whatsAppService;
        this.repository = repository;
    }

    // --- EXISTING DAILY REMINDER (Keep this) ---
    @Scheduled(cron = "0 0 20 * * *") 
    public void sendDailyChoreReminder() {
        // ... (Your existing code here) ...
    }

    // --- NEW: SUPABASE KEEPER (Runs every 3 days at midnight) ---
    // Cron Breakdown: 0 sec, 0 min, 0 hour, every 3rd day, any month, any day
    @Scheduled(cron = "0 0 0 */3 * *") 
    public void keepDatabaseAwake() {
        System.out.println("--- 🔌 DATABASE PING: Keeping Supabase Awake (" + LocalDateTime.now() + ") ---");
        
        // This simple query hits the DB and resets Supabase's 7-day inactivity timer
        long count = repository.count();
        
        System.out.println("--- ✅ PING SUCCESS. Member count: " + count + " ---");
    }
}