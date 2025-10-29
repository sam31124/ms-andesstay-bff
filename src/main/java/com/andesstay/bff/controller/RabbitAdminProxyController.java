package com.andesstay.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/rabbit")
@PreAuthorize("hasRole('Admin')")
public class RabbitAdminProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.rabbitadmin.url:http://localhost:8084}")
    private String rabbitAdminUrl;

    public RabbitAdminProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/topology/init")
    public ResponseEntity<?> initTopology() {
        return forward(HttpMethod.POST, "/api/rabbit/topology/init", null);
    }

    @GetMapping("/topology/status")
    public ResponseEntity<?> getTopologyStatus() {
        return forward(HttpMethod.GET, "/api/rabbit/topology/status", null);
    }

    @PostMapping("/queues")
    public ResponseEntity<?> createQueue(@RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, "/api/rabbit/queues", body);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<?> deleteQueue(@PathVariable String name) {
        return forward(HttpMethod.DELETE, "/api/rabbit/queues/" + name, null);
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<?> getQueue(@PathVariable String name) {
        return forward(HttpMethod.GET, "/api/rabbit/queues/" + name, null);
    }

    @PostMapping("/exchanges")
    public ResponseEntity<?> createExchange(@RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, "/api/rabbit/exchanges", body);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<?> deleteExchange(@PathVariable String name) {
        return forward(HttpMethod.DELETE, "/api/rabbit/exchanges/" + name, null);
    }

    @PostMapping("/bindings")
    public ResponseEntity<?> createBinding(@RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, "/api/rabbit/bindings", body);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<?> deleteBinding(@RequestBody Map<String, Object> body) {
        return forward(HttpMethod.DELETE, "/api/rabbit/bindings", body);
    }

    @PostMapping("/messages/test")
    public ResponseEntity<?> sendTestMessage(@RequestBody Map<String, Object> body) {
        return forward(HttpMethod.POST, "/api/rabbit/messages/test", body);
    }

    private ResponseEntity<?> forward(HttpMethod method, String path, Object body) {
        try {
            String url = rabbitAdminUrl + path;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(url, method, entity, Map.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", "ms-andesstay-rabbit-admin no disponible en " + rabbitAdminUrl,
                    "details", e.getMessage()
            ));
        }
    }
}
