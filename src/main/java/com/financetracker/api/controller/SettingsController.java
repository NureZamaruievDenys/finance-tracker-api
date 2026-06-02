package com.financetracker.api.controller;

import com.financetracker.api.model.User;
import com.financetracker.api.model.UserSettings;
import com.financetracker.api.repository.UserRepository;
import com.financetracker.api.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SettingsController {

    private final UserSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));
    }

    @GetMapping
    public ResponseEntity<UserSettings> getSettings(Principal principal) {
        User user = getCurrentUser(principal);

        UserSettings settings = settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSettings defaultSettings = new UserSettings();
                    defaultSettings.setUserId(user.getId());
                    return settingsRepository.save(defaultSettings);
                });

        return ResponseEntity.ok(settings);
    }

    @PutMapping
    public ResponseEntity<UserSettings> updateSettings(@RequestBody UserSettings updatedSettings, Principal principal) {
        User user = getCurrentUser(principal);

        UserSettings existingSettings = settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSettings newSettings = new UserSettings();
                    newSettings.setUserId(user.getId());
                    return newSettings;
                });

        existingSettings.setMainCurrency(updatedSettings.getMainCurrency());
        existingSettings.setSecondaryCurrency(updatedSettings.getSecondaryCurrency());
        existingSettings.setLanguage(updatedSettings.getLanguage());
        existingSettings.setTheme(updatedSettings.getTheme());
        existingSettings.setRoundingFormat(updatedSettings.getRoundingFormat());
        existingSettings.setFirstDayOfWeek(updatedSettings.getFirstDayOfWeek());
        existingSettings.setRemindDataEntry(updatedSettings.isRemindDataEntry());
        existingSettings.setWarnLimitExceeded(updatedSettings.isWarnLimitExceeded());
        existingSettings.setRemindUpdates(updatedSettings.isRemindUpdates());

        return ResponseEntity.ok(settingsRepository.save(existingSettings));
    }
}