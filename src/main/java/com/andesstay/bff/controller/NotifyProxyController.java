package com.andesstay.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/notify")
@PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Auditor')")
public class NotifyProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.notify.url:http://localhost:8083}")
    private String notifyServiceUrl;

    public NotifyProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getLogs() {
        return forward(HttpMethod.GET, "/api/notify/logs", null);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return forward(HttpMethod.GET, "/api/notify/stats", null);
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearLogs() {
        return forward(HttpMethod.POST, "/api/notify/clear", null);
    }

    @PostMapping("/simulate-send")
    public ResponseEntity<?> simulateSend(@RequestBody(required = false) Map<String, Object> body) {
        return forward(HttpMethod.POST, "/api/notify/simulate-send", body);
    }

    private ResponseEntity<?> forward(HttpMethod method, String path, Object body) {
        try {
            String url = notifyServiceUrl + path;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, method, entity, Map.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", "ms-andesstay-notify no disponible en " + notifyServiceUrl,
                    "details", e.getMessage()
            ));
        }
    }
}
