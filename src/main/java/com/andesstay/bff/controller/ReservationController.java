package com.andesstay.bff.controller;

import com.andesstay.bff.entity.Reservation;
import com.andesstay.bff.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationRepository reservationRepository;

    public ReservationController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Huesped')")
    public ResponseEntity<?> getReservations(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.toList());

        List<Reservation> reservations = reservationRepository.findAll();

        return ResponseEntity.ok(Map.of(
            "message", "Obtención exitosa de reservas desde Base de Datos",
            "user", authentication.getName(),
            "roles", roles,
            "data", reservations
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista')")
    public ResponseEntity<?> createReservation(@RequestBody Map<String, String> body) {
        // Soporta tanto los nombres en español como en inglés que envíe el cliente
        String huesped = body.getOrDefault("huesped", body.get("guestName"));
        String unidad = body.getOrDefault("unidad", body.get("room"));
        String estado = body.getOrDefault("estado", body.get("status"));

        if (huesped == null || huesped.isBlank() || unidad == null || unidad.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Los campos de huésped y unidad/habitación son obligatorios."
            ));
        }

        Reservation newReservation = new Reservation();
        newReservation.setHuesped(huesped.trim());
        newReservation.setUnidad(unidad.trim());
        // Por regla del ciclo de vida, si no se especifica, arranca en CREADA
        newReservation.setEstado((estado != null && !estado.isBlank()) ? estado.toUpperCase().trim() : "CREADA");

        Reservation savedReservation = reservationRepository.save(newReservation);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Reserva creada con éxito",
            "data", savedReservation
        ));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'status' es obligatorio."));
        }

        return reservationRepository.findById(id).map(reserva -> {
            // Regla de negocio AndesStay: No se puede hacer check-in sin CONFIRMAR
            if ("CHECKIN_PENDIENTE".equalsIgnoreCase(newStatus) && !"CONFIRMADA".equalsIgnoreCase(reserva.getEstado())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Regla de negocio violada: No se puede pasar a CHECKIN_PENDIENTE si la reserva no está CONFIRMADA."
                ));
            }

            reserva.setEstado(newStatus.toUpperCase());
            reservationRepository.save(reserva);
            return ResponseEntity.ok(Map.of(
                "message", "Estado de reserva actualizado con éxito",
                "data", reserva
            ));
        }).orElse(ResponseEntity.notFound().build());
    }
}