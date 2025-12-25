package com.family.chores.model;

public class FamilyMember {
    private String name;
    private String phoneNumber;
    private String role;
    
    // NEW FIELD
    private boolean lastTurnSkipped = false; 

    public FamilyMember() {
    }

    public FamilyMember(String name, String phoneNumber, String role) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    // ... existing getters and setters ...

    // NEW GETTER & SETTER
    public boolean isLastTurnSkipped() {
        return lastTurnSkipped;
    }

    public void setLastTurnSkipped(boolean lastTurnSkipped) {
        this.lastTurnSkipped = lastTurnSkipped;
    }
    
    // (Keep your other getters/setters for name, phoneNumber, role here)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}