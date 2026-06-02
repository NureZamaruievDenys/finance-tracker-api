package com.financetracker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategoryStatDto {
    private String category;
    private BigDecimal totalAmount;
}