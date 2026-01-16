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
    
    @PostConstruct
    public void init() {
        // --- REPLACE WITH REAL NUMBERS ---
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
    
    // --- NEW: Expose the list so we can print the schedule ---
    public List<FamilyMember> getFamilyMembers() {
        return familyMembers;
    }
    // ---------------------------------------------------------

    public FamilyMember getMemberToBlame() {
        int stepsBack = 1;
        while (stepsBack < familyMembers.size()) {
            int targetIndex = (currentIndex - stepsBack + familyMembers.size()) % familyMembers.size();
            FamilyMember candidate = familyMembers.get(targetIndex);
            
            if (!candidate.isLastTurnSkipped()) {
                return candidate;
            }
            System.out.println("Skipping " + candidate.getName() + " (History: Skipped Turn)");
            stepsBack++;
        }
        int fallbackIndex = (currentIndex - 1 + familyMembers.size()) % familyMembers.size();
        return familyMembers.get(fallbackIndex);
    }

    public void rotateTurn() {
        getCurrentMember().setLastTurnSkipped(false);
        currentIndex = (currentIndex + 1) % familyMembers.size();
        System.out.println("Turn rotated normally to: " + getCurrentMember().getName());
    }

    public void approveBypass() {
        getCurrentMember().setLastTurnSkipped(true);
        currentIndex = (currentIndex + 1) % familyMembers.size();
        System.out.println("Bypass approved. Turn moved to " + getCurrentMember().getName());

    }

    public String requestBypass(String phoneNumber, String reason) {
        FamilyMember current = getCurrentMember();
        if (!current.getPhoneNumber().equals(phoneNumber)) {
            return "It is not your turn, so you cannot skip.";
        }
        if ("Parent".equalsIgnoreCase(current.getRole()) || "Admin".equalsIgnoreCase(current.getRole())) {
            return "APPROVED_AUTO";
        }
        return "PENDING_APPROVAL";
    }
}