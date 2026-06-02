package com.financetracker.api.controller;

import com.financetracker.api.dto.ChatRequestDto;
import com.financetracker.api.model.User;
import com.financetracker.api.repository.UserRepository;
import com.financetracker.api.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiController {

    private final AiService aiService;
    private final UserRepository userRepository;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequestDto request, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        String aiResponse = aiService.generateResponse(user.getId(), request.getMessage());

        return ResponseEntity.ok(Map.of("response", aiResponse));
    }
}