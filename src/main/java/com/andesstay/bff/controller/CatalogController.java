package com.andesstay.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final RestTemplate restTemplate;

    @Value("${services.catalog.url:http://localhost:8082}")
    private String catalogServiceUrl;

    public CatalogController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/units")
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista')")
    public ResponseEntity<?> getUnits() {
        // Intenta consultar al microservicio independiente de catálogo
        try {
            String targetUrl = catalogServiceUrl + "/api/catalog/units";
            ResponseEntity<Map> response = restTemplate.getForEntity(targetUrl, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            }
        } catch (Exception e) {
            // Fallback a catálogo por defecto si el microservicio downstream no está activo
        }

        List<Map<String, Object>> units = List.of(
            Map.of("id", "U-01", "nombre", "Cabaña Vista Volcán", "tipo", "CABAÑA", "capacidad", 5, "tarifaBase", 95000, "disponible", true),
            Map.of("id", "U-02", "nombre", "Habitación 102 Standard", "tipo", "HABITACION", "capacidad", 2, "tarifaBase", 45000, "disponible", true),
            Map.of("id", "U-03", "nombre", "Lodge Bosque Nativo", "tipo", "LODGE", "capacidad", 6, "tarifaBase", 120000, "disponible", false)
        );
        return ResponseEntity.ok(Map.of("status", "success", "data", units));
    }
}