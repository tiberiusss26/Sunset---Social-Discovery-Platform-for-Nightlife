package com.nightout.service;

import com.nightout.domain.Role;
import com.nightout.domain.User;
import com.nightout.dto.*;
import com.nightout.exception.DuplicateResourceException;
import com.nightout.exception.ResourceNotFoundException;
import com.nightout.repository.RoleRepository;
import com.nightout.repository.UserRepository;
import com.nightout.security.CustomUserDetailsService;
import com.nightout.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        if (userRepository.existsByUsername(request.getUsername()))
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());

        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_USER"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        user.addRole(userRole);

        User saved = userRepository.save(user);
        log.info("New user registered: {} ({})", saved.getUsername(), saved.getId());
        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        log.info("User logged in: {} ({})", user.getUsername(), user.getId());
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails, user.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(UserSummary.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .roles(user.getRoles().stream()
                                .map(r -> r.getName().name()).toList())
                        .build())
                .build();
    }
}