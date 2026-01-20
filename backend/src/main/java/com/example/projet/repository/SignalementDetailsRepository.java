package com.example.project.repository;


import com.example.project.entity.SignalementDetails;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SignalementDetailsRepository extends JpaRepository<SignalementDetails, Long> {
}