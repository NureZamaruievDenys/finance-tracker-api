package com.financetracker.api.repository;

import com.financetracker.api.dto.CategoryStatDto;
import com.financetracker.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = :type")
    BigDecimal sumAmountByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Query("SELECT new com.financetracker.api.dto.CategoryStatDto(t.category, SUM(t.amount)) " +
            "FROM Transaction t " +
            "WHERE t.userId = :userId AND t.type = 'EXPENSE' " +
            "GROUP BY t.category " +
            "ORDER BY SUM(t.amount) DESC")
    List<CategoryStatDto> getExpenseStatsByCategory(@Param("userId") Long userId);

    @Query("SELECT new com.financetracker.api.dto.CategoryStatDto(t.category, SUM(t.amount)) " +
            "FROM Transaction t " +
            "WHERE t.userId = :userId AND t.type = 'INCOME' " +
            "GROUP BY t.category " +
            "ORDER BY SUM(t.amount) DESC")
    List<CategoryStatDto> getIncomeStatsByCategory(@Param("userId") Long userId);

    @Query("SELECT COALESCE(AVG(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = :type")
    BigDecimal getAverageAmountByType(@Param("userId") Long userId, @Param("type") String type);

    long countByUserId(Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (CAST(:startDate AS date) IS NULL OR t.transactionDate >= :startDate) " +
            "AND (CAST(:endDate AS date) IS NULL OR t.transactionDate <= :endDate) " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findFilteredTransactions(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    boolean existsByExternalId(String externalId);
}