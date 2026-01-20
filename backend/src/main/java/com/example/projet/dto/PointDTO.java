package com.example.projet.dto;


public class PointDTO {
public Long id;
public double lat;
public double lng;


public PointDTO(Long id, double lat, double lng) {
this.id = id;
this.lat = lat;
this.lng = lng;
}
}