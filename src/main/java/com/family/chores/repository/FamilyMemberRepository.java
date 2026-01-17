package com.family.chores.repository;

import com.family.chores.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, String> {

    // Find the person whose turn it currently is
    Optional<FamilyMember> findByIsTurnTrue();

    // Find a specific person by their rotation order
    Optional<FamilyMember> findByRotationOrder(int order);

    // Find the Admin (for permissions)
    FamilyMember findByRole(String role);
}