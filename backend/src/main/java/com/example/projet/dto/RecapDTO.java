package com.example.projet.dto;

public class RecapDTO {
    public int nbPoints;
    public double totalSurface;
    public double avancementPourcent;
    public double totalBudget;

    public RecapDTO(int nbPoints, double totalSurface, double avancementPourcent, double totalBudget) {
        this.nbPoints = nbPoints;
        this.totalSurface = totalSurface;
        this.avancementPourcent = avancementPourcent;
        this.totalBudget = totalBudget;
    }
}
