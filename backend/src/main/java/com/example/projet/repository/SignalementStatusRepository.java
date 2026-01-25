package com.example.projet.repository;

import com.example.projet.entity.SignalementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SignalementStatusRepository extends JpaRepository<SignalementStatus, Long> {

    Optional<SignalementStatus> findByIdSignalement(Long idSignalement);

    @Modifying
    @Query("UPDATE SignalementStatus ss SET ss.idStatut = :idStatut WHERE ss.idSignalement = :idSignalement")
    int updateStatut(@Param("idSignalement") Long idSignalement, @Param("idStatut") Integer idStatut);

    boolean existsByIdSignalement(Long idSignalement);
}
