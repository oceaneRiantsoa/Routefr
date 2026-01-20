package com.example.project.controller;


import com.example.project.dto.PointDTO;
import com.example.project.entity.SignalementDetails;
import com.example.project.repository.SignalementDetailsRepository;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController
@RequestMapping("/api/map")
@CrossOrigin(origins = "*")
public class MapController {
private final SignalementDetailsRepository repo;


public MapController(SignalementDetailsRepository repo) {
this.repo = repo;
}


@GetMapping("/points")
public List<PointDTO> getPoints() {
return repo.findAll().stream()
.map(s -> new PointDTO(s.getId(), s.getGeom().getY(), s.getGeom().getX()))
.toList();
}
}