package com.example.payment.api;

import com.example.payment.persistence.OrderEntity;
import com.example.payment.persistence.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final com.example.payment.service.OrderService orderService;

    public OrderController(com.example.payment.service.OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<com.example.payment.api.dto.OrderResponse> createOrder(@RequestBody com.example.payment.api.dto.OrderRequest req) {
        com.example.payment.api.dto.OrderResponse resp = orderService.createOrder(req);
        if (resp == null) return ResponseEntity.status(500).build();
        return ResponseEntity.created(URI.create("/api/v1/orders/" + resp.getId())).body(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.example.payment.api.dto.OrderResponse> getOrder(@PathVariable("id") String id) {
        com.example.payment.api.dto.OrderResponse resp = orderService.getOrder(id);
        if (resp == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(resp);
    }
}
