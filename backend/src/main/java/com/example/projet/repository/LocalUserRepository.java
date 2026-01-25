package com.example.projet.repository;


import com.example.projet.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface LocalUserRepository extends JpaRepository<LocalUser, Long> {
    List<LocalUser> findByAccountLockedTrue();
    Optional<LocalUser> findById(Long id);
    Optional<LocalUser> findByEmail(String email);
    Optional<LocalUser> findByFirebaseUid(String firebaseUid);
    boolean existsByEmail(String email);
   

}