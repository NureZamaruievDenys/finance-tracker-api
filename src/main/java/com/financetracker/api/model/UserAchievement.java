package com.financetracker.api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(
        name = "user_achievements",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "achievement_type"})}
)
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_type", nullable = false)
    private AchievementType achievementType;

    @Column(name = "unlocked_at", nullable = false)
    private LocalDate unlockedAt = LocalDate.now();
}