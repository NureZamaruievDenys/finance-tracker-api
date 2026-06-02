package com.financetracker.api.service;

import com.financetracker.api.dto.AchievementDto;
import com.financetracker.api.dto.AchievementProgressDto;
import com.financetracker.api.model.AchievementType;
import com.financetracker.api.model.UserAchievement;
import com.financetracker.api.repository.TransactionRepository;
import com.financetracker.api.repository.UserAchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final UserAchievementRepository achievementRepository;
    private final TransactionRepository transactionRepository;

    public void checkAndUnlockAchievements(Long userId) {
        // Проверка ачивки "Перші кроки" (Додано першу транзакцію)
        if (!achievementRepository.existsByUserIdAndAchievementType(userId, AchievementType.FIRST_STEPS)) {
            long txCount = transactionRepository.countByUserId(userId);
            if (txCount >= 1) {
                unlock(userId, AchievementType.FIRST_STEPS);
            }
        }

        // Сюда в будущем добавятся проверки остальных ачивок:
        // Например, для INVESTOR: если сумма доходов - расходов > 10000, вызываем unlock(...)
    }

    private void unlock(Long userId, AchievementType type) {
        UserAchievement ua = new UserAchievement();
        ua.setUserId(userId);
        ua.setAchievementType(type);
        achievementRepository.save(ua);
    }

    public AchievementProgressDto getProgress(Long userId) {
        List<UserAchievement> unlocked = achievementRepository.findAllByUserId(userId);
        Map<AchievementType, LocalDate> unlockedMap = unlocked.stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementType, UserAchievement::getUnlockedAt));

        List<AchievementDto> allAchievements = new ArrayList<>();

        allAchievements.add(createDto(AchievementType.FIRST_STEPS, "Перші кроки", "Додано першу транзакцію", unlockedMap));
        allAchievements.add(createDto(AchievementType.THRIFT, "Ощадливість", "Тиждень без імпульсивних покупок", unlockedMap));
        allAchievements.add(createDto(AchievementType.DISCIPLINE, "Дисципліна", "3 місяці ведення бюджету", unlockedMap));
        allAchievements.add(createDto(AchievementType.PLANNING, "Планування", "Створено перший бюджет", unlockedMap));
        allAchievements.add(createDto(AchievementType.STABILITY, "Стабільність", "30 днів поспіль ведення обліку", unlockedMap));
        allAchievements.add(createDto(AchievementType.INVESTOR, "Інвестор", "Відкладено 10 000 грн", unlockedMap));

        int unlockedCount = unlocked.size();
        int totalCount = AchievementType.values().length;
        double percentage = ((double) unlockedCount / totalCount) * 100;

        return new AchievementProgressDto(unlockedCount, totalCount, percentage, allAchievements);
    }

    private AchievementDto createDto(AchievementType type, String title, String desc, Map<AchievementType, LocalDate> unlockedMap) {
        boolean isUnlocked = unlockedMap.containsKey(type);
        LocalDate date = isUnlocked ? unlockedMap.get(type) : null;
        return new AchievementDto(type.name(), title, desc, isUnlocked, date);
    }
}
