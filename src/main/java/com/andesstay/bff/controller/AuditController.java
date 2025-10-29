package com.andesstay.bff.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @GetMapping("/timeline")
    @PreAuthorize("hasAnyRole('Admin', 'Auditor')")
    public ResponseEntity<?> getTimeline() {
        List<Map<String, Object>> events = List.of(
            Map.of("eventId", "EVT-901", "action", "RESERVA_CREADA", "user", "sa.urzua@duocuc.cl", "timestamp", "2026-09-14 10:15:22", "detalle", "Reserva ID 101 creada"),
            Map.of("eventId", "EVT-902", "action", "RESERVA_CONFIRMADA", "user", "recepcion@andesstay.cl", "timestamp", "2026-09-14 11:00:10", "detalle", "Confirmación y bloqueo de unidad")
        );
        return ResponseEntity.ok(Map.of("status", "success", "timeline", events));
    }
}