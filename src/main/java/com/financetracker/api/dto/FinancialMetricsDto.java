package com.financetracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class FinancialMetricsDto {
    private BigDecimal averageIncome;
    private BigDecimal averageExpense;
    private BigDecimal savingsRate;
    private String topExpenseCategory;
}