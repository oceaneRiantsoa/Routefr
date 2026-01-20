package com.example.projet.dto;

import java.time.LocalDateTime;

public class PointDetailDTO {
    public Long id;
    public double lat;
    public double lng;
    public String probleme;
    public LocalDateTime dateSignalement;
    public String status;
    public double surface;
    public double budget;
    public String entreprise;
    public String commentaires;

    public PointDetailDTO(Long id, double lat, double lng, String probleme,
                          LocalDateTime dateSignalement, Integer idStatut,
                          double surface, double coutParM2, String entreprise,
                          String commentaires) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.probleme = probleme;
        this.dateSignalement = dateSignalement;
        this.status = convertStatut(idStatut);
        this.surface = surface;
        this.budget = surface * coutParM2;
        this.entreprise = entreprise;
        this.commentaires = commentaires;
    }

    private String convertStatut(Integer idStatut) {
        if (idStatut == null) return "Nouveau";
        return switch (idStatut) {
            case 10 -> "Nouveau";
            case 20 -> "En cours";
            case 30 -> "Terminé";
            default -> "Inconnu";
        };
    }
}
