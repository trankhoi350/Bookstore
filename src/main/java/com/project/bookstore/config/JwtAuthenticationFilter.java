package com.project.bookstore.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        System.out.println(">>> [JWT] " + request.getMethod() + " " + request.getServletPath());

        // Skip filtering if shouldNotFilter returns true
        if (shouldNotFilter(request)) {
            System.out.println(">>> [JWT] Skipping JWT processing for this request");
            filterChain.doFilter(request, response);
            return;
        }

        // Log the raw header
        String authHeader = request.getHeader("Authorization");
        System.out.println(">>> [JWT] Authorization header = " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract and process the JWT
        final String jwt = authHeader.substring(7);
        try {
            final String userEmail = jwtService.extractUsername(jwt);
            System.out.println("Extracted email: " + userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    System.out.println("Authentication successful for user: " + userEmail);
                } else {
                    System.out.println("Token validation failed");
                }
            }
        } catch (Exception e) {
            System.out.println("Error processing JWT: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        System.out.println("Checking if should filter path: " + path + ", method: " + method);

        // Always skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // Define paths that should not be filtered
        boolean shouldSkip =
                // Auth endpoints
                path.startsWith("/api/v1/auth/") ||

                        // Book search endpoints
                        path.equals("/api/bookstore/search") ||
                        path.equals("/api/bookstore/amazon") ||

                        // Cover images
                        path.startsWith("/covers/");

        // IMPORTANT: Remove the match for "/api/v1/cart/**" from the skip list
        // as we want to authenticate these requests

        System.out.println("Should skip filter: " + shouldSkip);
        return shouldSkip;
    }
}