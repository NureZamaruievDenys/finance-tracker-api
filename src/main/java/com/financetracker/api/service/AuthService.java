package com.financetracker.api.service;

import com.financetracker.api.dto.AuthResponse;
import com.financetracker.api.dto.LoginRequest;
import com.financetracker.api.dto.RegisterRequest;
import com.financetracker.api.model.User;
import com.financetracker.api.repository.UserRepository;
import com.financetracker.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email вже використовується");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);

        // Повертаємо токен + об'єкт юзера
        return new AuthResponse(
                jwtToken,
                new AuthResponse.UserDto(user.getName(), user.getEmail())
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String jwtToken = jwtService.generateToken(user);

        // Повертаємо токен + об'єкт юзера
        return new AuthResponse(
                jwtToken,
                new AuthResponse.UserDto(user.getName(), user.getEmail())
        );
    }
}