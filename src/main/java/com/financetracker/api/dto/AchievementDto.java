package com.financetracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class AchievementDto {
    private String type;
    private String title;
    private String description;
    private boolean isUnlocked;
    private LocalDate unlockedAt;
}
