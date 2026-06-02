package com.financetracker.api.repository;

import com.financetracker.api.model.AchievementType;
import com.financetracker.api.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findAllByUserId(Long userId);
    boolean existsByUserIdAndAchievementType(Long userId, AchievementType type);
}