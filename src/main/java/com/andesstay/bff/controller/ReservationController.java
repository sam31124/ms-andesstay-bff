package com.andesstay.bff.controller;

import com.andesstay.bff.entity.Reservation;
import com.andesstay.bff.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final RestTemplate restTemplate;

    @Value("${services.reservations.url:http://localhost:8081}")
    private String reservationsServiceUrl;

    public ReservationController(ReservationRepository reservationRepository, RestTemplate restTemplate) {
        this.reservationRepository = reservationRepository;
        this.restTemplate = restTemplate;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Huesped')")
    public ResponseEntity<?> getReservations(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.toList());

        // Intenta consultar al microservicio independiente de reservas
        try {
            String targetUrl = reservationsServiceUrl + "/api/reservations";
            ResponseEntity<Map> response = restTemplate.getForEntity(targetUrl, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return ResponseEntity.ok(Map.of(
                    "message", "Obtención exitosa de reservas desde microservicio ms-andesstay-reservations",
                    "user", authentication.getName(),
                    "roles", roles,
                    "data", body.get("data")
                ));
            }
        } catch (Exception e) {
            // Fallback a repositorio local si el microservicio downstream no está activo
        }

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
        // Intenta reenviar al microservicio independiente
        try {
            String targetUrl = reservationsServiceUrl + "/api/reservations";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(targetUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
            }
        } catch (Exception e) {
            // Fallback local
        }

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

        // Intenta reenviar al microservicio independiente
        try {
            String targetUrl = reservationsServiceUrl + "/api/reservations/" + id + "/status";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(targetUrl, HttpMethod.PUT, request, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            // Fallback local
        }

        return reservationRepository.findById(id).map(reserva -> {
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