package com.family.chores.model;

public class FamilyMember {
    private String name;
    private String phoneNumber;
    private String role; // "Admin", "Parent", "Child"

    // Default Constructor
    public FamilyMember() {
    }

    // --- THIS IS THE MISSING CONSTRUCTOR ---
    public FamilyMember(String name, String phoneNumber, String role) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // --- THIS IS THE MISSING METHOD ---
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}