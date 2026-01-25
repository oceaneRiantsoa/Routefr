package com.example.projet.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseDTO {
    private Long id;
    private String nomEntreprise;
    private String localisation;
    private String contact;
}
