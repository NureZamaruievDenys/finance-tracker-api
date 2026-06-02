package com.financetracker.api.controller;

import com.financetracker.api.model.User;
import com.financetracker.api.repository.UserRepository;
import com.financetracker.api.service.MonobankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/mono")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MonobankController {

    private final MonobankService monobankService;
    private final UserRepository userRepository;

    @PostMapping("/sync")
    public ResponseEntity<?> syncWithMono(@RequestBody Map<String, String> body, Principal principal) {
        String monoToken = body.get("monoToken");

        if (monoToken == null || monoToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Токен не надано"));
        }

        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        String result = monobankService.syncTransactions(user.getId(), monoToken);

        return ResponseEntity.ok(Map.of("message", result));
    }
}