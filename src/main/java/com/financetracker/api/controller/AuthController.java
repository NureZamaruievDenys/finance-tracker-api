package com.financetracker.api.controller;

import com.financetracker.api.model.User;
import com.financetracker.api.repository.UserRepository;
import com.financetracker.api.security.JwtService;
import com.financetracker.api.dto.GoogleAuthRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // --- DTO классы для приема данных ---
    @Data
    public static class AuthRequest {
        private String email;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String email;
        private String password;
        private String name;
    }
    // ------------------------------------

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email вже використовується"));
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Шифруем пароль!
        user.setName(request.getName());

        userRepository.save(user);

        // Генерируем токен и отдаем его
        String jwtToken = jwtService.generateToken(user);
        return ResponseEntity.ok(Map.of("token", jwtToken));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // Эта строка проверит правильность пароля под капотом (через AuthenticationManager)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Если пароль подошел, достаем юзера и генерируем токен
            User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
            String jwtToken = jwtService.generateToken(user);

            return ResponseEntity.ok(Map.of("token", jwtToken));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Невірний email або пароль"));
        }
    }

    @Value("${app.google.client-id}")
    private String googleClientId;

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleAuthRequest request) {
        try {
            // 1. Налаштовуємо верифікатор Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // 2. Перевіряємо токен, який прийшов з фронтенду
            GoogleIdToken idToken = verifier.verify(request.getToken());

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                // 3. Дістаємо дані користувача з токена
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");

                // 4. Шукаємо юзера в базі
                Optional<User> userOptional = userRepository.findByEmail(email);
                User user;

                if (userOptional.isPresent()) {
                    // Якщо юзер вже є (реєструвався раніше або вже заходив через гугл)
                    user = userOptional.get();
                } else {
                    // Якщо юзера немає — реєструємо нового
                    user = new User();
                    user.setEmail(email);
                    user.setName(name);
                    user.setAvatarUrl(pictureUrl);

                    // Оскільки пароль обов'язковий у БД, але юзер входить через гугл,
                    // генеруємо складний випадковий пароль, який він ніколи не дізнається.
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

                    userRepository.save(user);
                }

                // 5. Генеруємо НАШ локальний JWT токен для цього юзера
                String localJwtToken = jwtService.generateToken(user);

                // 6. Віддаємо фронтенду
                return ResponseEntity.ok(Map.of("token", localJwtToken));

            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Недійсний Google токен"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Помилка при перевірці Google авторизації"));
        }
    }
}
