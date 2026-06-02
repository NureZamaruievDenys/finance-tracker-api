package com.financetracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class AchievementProgressDto {
    private int unlockedCount;
    private int totalCount;
    private double progressPercentage;
    private List<AchievementDto> achievements;
}
