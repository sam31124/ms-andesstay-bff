package com.andesstay.bff.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @GetMapping("/kpis")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<?> getKpis() {
        Map<String, Object> kpis = Map.of(
            "reservasPorHora", 14,
            "tiempoCicloPromedioMin", 8.5,
            "ocupacionActivaPorcentaje", 78.4,
            "totalIngresosEstadia", 4520000
        );
        return ResponseEntity.ok(Map.of("status", "success", "kpis", kpis));
    }
}