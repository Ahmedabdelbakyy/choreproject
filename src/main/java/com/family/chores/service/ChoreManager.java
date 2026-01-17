package com.family.chores.service;

import com.family.chores.model.FamilyMember;
import com.family.chores.repository.FamilyMemberRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 

import java.util.List;

@Service
public class ChoreManager {

    private final FamilyMemberRepository repository;

    public ChoreManager(FamilyMemberRepository repository) {
        this.repository = repository;
    }

    // --- 1. INITIALIZATION (Runs once when server starts) ---
    @PostConstruct
    public void init() {
        // Only create users if the database is empty!
        if (repository.count() == 0) {
            System.out.println("--- Seeding Database with Family Members ---");
            
            // CRITICAL: Phone numbers act as IDs, so they MUST be unique.
            // TODO: Replace these with real numbers before deploying!
            saveInitialMember("Ahmed", "+201099843408", "Admin", 1);
            saveInitialMember("Ashraf", "+201001482564", "Parent", 2);
            saveInitialMember("Omar",   "+201555909711", "Child", 3);
            saveInitialMember("Mohamed","+201005196654", "Child", 4);

            // Set the first person (Ahmed) as the active turn
            FamilyMember first = repository.findByRotationOrder(1).orElseThrow();
            first.setTurn(true);
            repository.save(first);
            System.out.println("Database seeded. First turn assigned to: " + first.getName());
        } else {
            System.out.println("--- Database already has data. Skipping seed. ---");
        }
    }

    private void saveInitialMember(String name, String phone, String role, int order) {
        repository.save(new FamilyMember(name, phone, role, order));
    }

    // --- 2. DATA ACCESS ---
    public List<FamilyMember> getFamilyMembers() {
        // Return list sorted by rotation order (1, 2, 3, 4)
        return repository.findAll(Sort.by("rotationOrder"));
    }

    public FamilyMember getCurrentMember() {
        // The DB knows whose turn it is!
        return repository.findByIsTurnTrue()
                .orElseThrow(() -> new RuntimeException("DB Error: No one has the turn!"));
    }

    public FamilyMember getAdmin() {
        // Find the admin. If multiple, this logic might need adjustment, but safe for now.
        FamilyMember admin = repository.findByRole("Admin");
        return (admin != null) ? admin : getFamilyMembers().get(0);
    }

    // --- 3. CORE LOGIC (Transactional = Safe Saves) ---
    
    @Transactional
    public void rotateTurn() {
        FamilyMember current = getCurrentMember();
        List<FamilyMember> allMembers = getFamilyMembers();

        // Math to find the next person (Order is 1-based, List is 0-based)
        int currentIndex = current.getRotationOrder() - 1; 
        int nextIndex = (currentIndex + 1) % allMembers.size();
        FamilyMember next = allMembers.get(nextIndex);

        // Swap the flag
        current.setTurn(false);
        current.setLastTurnSkipped(false); // Reset history
        next.setTurn(true);

        // Save both changes instantly
        repository.save(current);
        repository.save(next);
        
        System.out.println("Turn rotated from " + current.getName() + " to " + next.getName());
    }

    @Transactional
    public void approveBypass() {
        FamilyMember current = getCurrentMember();
        List<FamilyMember> allMembers = getFamilyMembers();
        
        int currentIndex = current.getRotationOrder() - 1; 
        int nextIndex = (currentIndex + 1) % allMembers.size();
        FamilyMember next = allMembers.get(nextIndex);

        // Logic: Current skips (flag = true), Next takes over
        current.setTurn(false);
        current.setLastTurnSkipped(true); // Remember they skipped!
        next.setTurn(true);

        repository.save(current);
        repository.save(next);
        
        System.out.println("Bypass Approved. " + current.getName() + " skipped.");
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

    public FamilyMember getMemberToBlame() {
        // Loads everyone to calculate the history logic
        List<FamilyMember> members = getFamilyMembers();
        FamilyMember current = getCurrentMember();
        
        int currentIndex = current.getRotationOrder() - 1;
        int stepsBack = 1;

        // Trace back to find who actually worked last
        while (stepsBack < members.size()) {
            int targetIndex = (currentIndex - stepsBack + members.size()) % members.size();
            FamilyMember candidate = members.get(targetIndex);
            
            if (!candidate.isLastTurnSkipped()) {
                return candidate;
            }
            stepsBack++;
        }
        
        return members.get((currentIndex - 1 + members.size()) % members.size());
    }
}