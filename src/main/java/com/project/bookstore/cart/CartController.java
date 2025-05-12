package com.project.bookstore.cart;

import com.project.bookstore.config.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> getCart(@RequestHeader("Authorization") String token) {
        try {
            //Remove the "Bearer " prefix if present
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            System.out.println("[GET] Processing cart request with token: " + token);
            String email = jwtService.extractUsername(token);
            System.out.println("Extracted email: " + email);

            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            Cart cart = cartService.getUserCart(email);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            System.err.println("Error in getCart: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching cart: " + e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestHeader("Authorization") String token,
                                       @RequestBody CartItemRequest request) {
        try {
            System.out.println("Received add to cart request: " + request);

            //Remove the "Bearer " prefix if present
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            System.out.println("[POST] Received token: " + token);
            String email = jwtService.extractUsername(token);
            System.out.println("Extracted email: " + email);

            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            // Enhanced logging
            System.out.println("Adding item to cart for user: " + email);
            System.out.println("Item details: " +
                    "Title: " + request.getExternalTitle() +
                    ", Author: " + request.getExternalAuthor() +
                    ", Quantity: " + request.getQuantity());

            Cart cart = cartService.addItemToCart(email, request);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            System.err.println("Error in addToCart: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding to cart: " + e.getMessage());
        }
    }
}
