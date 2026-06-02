package com.financetracker.api.controller;

import com.financetracker.api.dto.CategoryStatDto;
import com.financetracker.api.dto.FinancialMetricsDto;
import com.financetracker.api.dto.SummaryStatDto;
import com.financetracker.api.model.User;
import com.financetracker.api.repository.TransactionRepository;
import com.financetracker.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryStatDto> getSummary(Principal principal) {
        User user = getCurrentUser(principal);

        BigDecimal income = transactionRepository.sumAmountByUserIdAndType(user.getId(), "INCOME");
        BigDecimal expense = transactionRepository.sumAmountByUserIdAndType(user.getId(), "EXPENSE");
        BigDecimal balance = income.subtract(expense);

        return ResponseEntity.ok(new SummaryStatDto(income, expense, balance));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryStatDto>> getCategoryStats(Principal principal) {
        User user = getCurrentUser(principal);
        return ResponseEntity.ok(transactionRepository.getExpenseStatsByCategory(user.getId()));
    }

    @GetMapping("/incomes")
    public ResponseEntity<List<CategoryStatDto>> getIncomeStats(Principal principal) {
        User user = getCurrentUser(principal);
        return ResponseEntity.ok(transactionRepository.getIncomeStatsByCategory(user.getId()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<FinancialMetricsDto> getMetrics(Principal principal) {
        User user = getCurrentUser(principal);
        Long userId = user.getId();

        BigDecimal avgIncome = transactionRepository.getAverageAmountByType(userId, "INCOME");
        BigDecimal avgExpense = transactionRepository.getAverageAmountByType(userId, "EXPENSE");

        BigDecimal totalIncome = transactionRepository.sumAmountByUserIdAndType(userId, "INCOME");
        BigDecimal totalExpense = transactionRepository.sumAmountByUserIdAndType(userId, "EXPENSE");

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal saved = totalIncome.subtract(totalExpense);
            savingsRate = saved.divide(totalIncome, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        List<CategoryStatDto> expenses = transactionRepository.getExpenseStatsByCategory(userId);
        String topCategory = expenses.isEmpty() ? "Немає витрат" : expenses.get(0).getCategory();

        return ResponseEntity.ok(new FinancialMetricsDto(avgIncome, avgExpense, savingsRate, topCategory));
    }
}