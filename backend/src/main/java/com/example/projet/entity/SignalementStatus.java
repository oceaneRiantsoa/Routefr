package com.example.projet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "signalement_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_signalement")
    private Integer idSignalement;

    @Column(name = "idstatut")
    private Integer idStatut;
}
