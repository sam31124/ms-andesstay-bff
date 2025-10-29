package com.andesstay.bff.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    @GetMapping("/units")
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista')")
    public ResponseEntity<?> getUnits() {
        List<Map<String, Object>> units = List.of(
            Map.of("id", "U-01", "nombre", "Cabaña Vista Volcán", "tipo", "CABAÑA", "capacidad", 5, "tarifaBase", 95000, "disponible", true),
            Map.of("id", "U-02", "nombre", "Habitación 102 Standard", "tipo", "HABITACION", "capacidad", 2, "tarifaBase", 45000, "disponible", true),
            Map.of("id", "U-03", "nombre", "Lodge Bosque Nativo", "tipo", "LODGE", "capacidad", 6, "tarifaBase", 120000, "disponible", false)
        );
        return ResponseEntity.ok(Map.of("status", "success", "data", units));
    }
}