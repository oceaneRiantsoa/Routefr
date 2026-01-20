package com.example.projet.controller;

import com.example.projet.dto.PointDetailDTO;
import com.example.projet.dto.RecapDTO;
import com.example.projet.repository.SignalementDetailsRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
    public List<PointDetailDTO> getPoints() {
        return repo.findAllPointsWithDetails().stream()
            .map(row -> new PointDetailDTO(
                ((Number) row[0]).longValue(),           // id
                ((Number) row[1]).doubleValue(),         // lat
                ((Number) row[2]).doubleValue(),         // lng
                (String) row[3],                         // probleme
                row[4] != null ? ((Timestamp) row[4]).toLocalDateTime() : null, // date
                row[5] != null ? ((Number) row[5]).intValue() : null,           // statut
                row[6] != null ? ((Number) row[6]).doubleValue() : 0,           // surface
                row[7] != null ? ((Number) row[7]).doubleValue() : 0,           // cout_par_m2
                (String) row[8],                         // entreprise
                (String) row[9]                          // commentaires
            ))
            .toList();
    }

    @GetMapping("/recap")
    public RecapDTO getRecap() {
        List<Object[]> result = repo.getRecapitulation();
        if (result == null || result.isEmpty()) {
            return new RecapDTO(0, 0, 0, 0);
        }
        Object[] row = result.get(0);
        return new RecapDTO(
            row[0] != null ? ((Number) row[0]).intValue() : 0,       // nb_points
            row[1] != null ? ((Number) row[1]).doubleValue() : 0,    // total_surface
            row[2] != null ? ((Number) row[2]).doubleValue() : 0,    // avancement_pourcent
            row[3] != null ? ((Number) row[3]).doubleValue() : 0     // total_budget
        );
    }
}