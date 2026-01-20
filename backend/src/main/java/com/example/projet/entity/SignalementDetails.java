package com.example.project.entity;


import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;


@Entity
@Table(name = "signalement_details")
public class SignalementDetails {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;


@Column(columnDefinition = "geography(Point,4326)")
private Point geom;


// getters et setters
public Long getId() { return id; }
public void setId(Long id) { this.id = id; }
public Point getGeom() { return geom; }
public void setGeom(Point geom) { this.geom = geom; }
}