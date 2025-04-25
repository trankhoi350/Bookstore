package com.project.bookstore.cart;

import com.project.bookstore.config.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@CrossOrigin("http://localhost:5173")
public class CartController {
    private final CartService cartService;
    private final JwtService jwtService;

    @Autowired
    public CartController(CartService cartService, JwtService jwtService) {
        this.cartService = cartService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(@RequestHeader("Authorization") String token) {
        //Remove the "Bearer " prefix if present
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String email = jwtService.extractUsername(token);
        Cart cart = cartService.getUserCart(email);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> addToCart(@RequestHeader("Authorization") String token,
                                          @RequestBody CartItemRequest request) {
        //Remove the "Bearer " prefix if present
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        System.out.println("Received token: " + token);
        String email = jwtService.extractUsername(token);
        Cart cart = cartService.addItemToCart(email, request);
        return ResponseEntity.ok(cart);
    }
}
