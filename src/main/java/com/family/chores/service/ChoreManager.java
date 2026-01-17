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

    // --- 1. INITIALIZATION ---
    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            System.out.println("--- Seeding Database with Family Members ---");

            // Pull real phone numbers from Environment Variables
            String phoneAhmed = System.getenv("PHONE_AHMED");
            String phoneAshraf = System.getenv("PHONE_ASHRAF");
            String phoneOmar = System.getenv("PHONE_OMAR");
            String phoneMohamed = System.getenv("PHONE_MOHAMED");

            // Safety Check: If user forgot to set variables, print a warning (or handle gracefully)
            if (phoneAhmed == null) System.err.println("WARNING: PHONE_AHMED is missing!");

            
            saveInitialMember("Ashraf",  phoneAshraf,  "Parent", 1);
            saveInitialMember("Ahmed",   phoneAhmed,   "Admin",  2);
            saveInitialMember("Omar",    phoneOmar,    "Child",  3);
            saveInitialMember("Mohamed", phoneMohamed, "Child",  4);

            FamilyMember first = repository.findByRotationOrder(1).orElseThrow();
            first.setTurn(true);
            repository.save(first);
            System.out.println("Database seeded. First turn assigned to: " + first.getName());
        }
    }

    private void saveInitialMember(String name, String phone, String role, int order) {
        repository.save(new FamilyMember(name, phone, role, order));
    }

    // --- 2. DATA ACCESS ---
    public List<FamilyMember> getFamilyMembers() {
        return repository.findAll(Sort.by("rotationOrder"));
    }

    public FamilyMember getCurrentMember() {
        return repository.findByIsTurnTrue()
                .orElseThrow(() -> new RuntimeException("DB Error: No one has the turn!"));
    }

    public FamilyMember getAdmin() {
        FamilyMember admin = repository.findByRole("Admin");
        return (admin != null) ? admin : getFamilyMembers().get(0);
    }

    // --- 3. CORE LOGIC ---
    @Transactional
    public void rotateTurn() {
        FamilyMember current = getCurrentMember();
        List<FamilyMember> allMembers = getFamilyMembers();

        int currentIndex = current.getRotationOrder() - 1; 
        int nextIndex = (currentIndex + 1) % allMembers.size();
        FamilyMember next = allMembers.get(nextIndex);

        current.setTurn(false);
        current.setLastTurnSkipped(false); 
        next.setTurn(true);

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

        current.setTurn(false);
        current.setLastTurnSkipped(true); 
        next.setTurn(true);

        repository.save(current);
        repository.save(next);
        
        System.out.println("Bypass Approved. " + current.getName() + " skipped.");
    }

    public String requestBypass(String phoneNumber, String reason) {
    FamilyMember current = getCurrentMember();
    
    if (!current.getPhoneNumber().equals(phoneNumber)) {
        return "🚫 *Hold on!* \nIt is not your turn, so you cannot skip. Nice try! 😉";
    }
        
        if ("Parent".equalsIgnoreCase(current.getRole()) || "Admin".equalsIgnoreCase(current.getRole())) {
            return "APPROVED_AUTO";
        }
        return "PENDING_APPROVAL";
    }

    public FamilyMember getMemberToBlame() {
        List<FamilyMember> members = getFamilyMembers();
        FamilyMember current = getCurrentMember();
        
        int currentIndex = current.getRotationOrder() - 1;
        int stepsBack = 1;

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