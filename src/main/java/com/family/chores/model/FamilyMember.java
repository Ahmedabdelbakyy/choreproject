package com.family.chores.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "family_members")
public class FamilyMember {

    @Id
    private String phoneNumber; // Unique ID
    
    private String name;
    private String role;
    private boolean lastTurnSkipped = false;
    
    // NEW: Determines the order (1, 2, 3...)
    private int rotationOrder; 
    
    // NEW: The database remembers whose turn it is!
    private boolean isTurn = false;

    public FamilyMember() {
    }

    public FamilyMember(String name, String phoneNumber, String role, int rotationOrder) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.rotationOrder = rotationOrder;
    }

    // --- Getters and Setters ---
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isLastTurnSkipped() { return lastTurnSkipped; }
    public void setLastTurnSkipped(boolean lastTurnSkipped) { this.lastTurnSkipped = lastTurnSkipped; }

    public int getRotationOrder() { return rotationOrder; }
    public void setRotationOrder(int rotationOrder) { this.rotationOrder = rotationOrder; }

    public boolean isTurn() { return isTurn; }
    public void setTurn(boolean turn) { isTurn = turn; }
}