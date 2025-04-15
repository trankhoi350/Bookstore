package com.project.bookstore.order;

import com.project.bookstore.config.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;
    private final JwtService jwtService;

    @Autowired
    public OrderController(OrderService orderService, JwtService jwtService) {
        this.orderService = orderService;
        this.jwtService = jwtService;
    }

    @PostMapping("/place")
    public ResponseEntity<Order> getUserOrder(@RequestHeader String token) {
        String email = jwtService.extractUsername(token);
        Order order = orderService.getUserOrder(email);
        return ResponseEntity.ok(order);
    }
}
