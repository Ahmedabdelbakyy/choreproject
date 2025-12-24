package com.family.chores.service;

import com.family.chores.model.FamilyMember;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChoreManager {
    private final List<FamilyMember> familyMembers = new ArrayList<>();
    private int currentIndex = 0;
    
    // Tracks if the previous turn was skipped so we know who to blame
    private boolean lastTurnWasSkipped = false;

    @PostConstruct
    public void init() {
        // --- IMPORTANT: REPLACE THESE WITH YOUR REAL NUMBERS ---
        // Ensure format is exactly like "+201xxxxxxxxx" (Country code + Number)
        familyMembers.add(new FamilyMember("Ahmed", "+201099843408", "Admin"));
        familyMembers.add(new FamilyMember("Ashraf", "+201099843408", "Parent"));
        familyMembers.add(new FamilyMember("Omar", "+201099843408", "Child"));
        familyMembers.add(new FamilyMember("Mohamed", "+201099843408", "Child"));
    }

    public FamilyMember getAdmin() {
        return familyMembers.stream()
                .filter(m -> "Admin".equalsIgnoreCase(m.getRole()))
                .findFirst()
                .orElse(familyMembers.get(0));
    }

    public FamilyMember getCurrentMember() {
        return familyMembers.get(currentIndex);
    }

    // NEW LOGIC: Determine who is responsible for unfinished work
    public FamilyMember getMemberToBlame() {
        int stepsBack = 1;
        
        if (lastTurnWasSkipped) {
            stepsBack = 2; // Jump over the skipped person to blame the one before them
            System.out.println("Last turn was skipped. Blaming the person from 2 days ago.");
        }

        // Circular math to go backwards safely
        int targetIndex = (currentIndex - stepsBack + familyMembers.size()) % familyMembers.size();
        return familyMembers.get(targetIndex);
    }

    // LOGIC: Normal daily rotation
    public void rotateTurn() {
        currentIndex = (currentIndex + 1) % familyMembers.size();
        lastTurnWasSkipped = false; // Reset flag on normal rotation
        System.out.println("Turn rotated normally to: " + getCurrentMember().getName());
    }

    // LOGIC: Admin Approved Bypass (Skip)
    public void approveBypass() {
        currentIndex = (currentIndex + 1) % familyMembers.size();
        lastTurnWasSkipped = true; // Mark this turn as skipped
        System.out.println("Bypass approved. Turn moved to " + getCurrentMember().getName() + " (Previous was Skipped)");
    }

    // LOGIC: Request Bypass
    public String requestBypass(String phoneNumber, String reason) {
        FamilyMember current = getCurrentMember();
        
        // Strict check: Only the current user can ask to skip
        if (!current.getPhoneNumber().equals(phoneNumber)) {
            return "It is not your turn, so you cannot skip.";
        }
        
        // Auto-approve for Parents/Admins
        if ("Parent".equalsIgnoreCase(current.getRole()) || "Admin".equalsIgnoreCase(current.getRole())) {
            approveBypass();
            return "APPROVED_AUTO";
        }
        
        return "PENDING_APPROVAL";
    }
}