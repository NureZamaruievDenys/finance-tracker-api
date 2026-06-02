package com.financetracker.api.controller;

import com.financetracker.api.model.Transaction;
import com.financetracker.api.model.User;
import com.financetracker.api.repository.TransactionRepository;
import com.financetracker.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final com.financetracker.api.service.AchievementService achievementService;

    private User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAll(
            Principal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        User user = getCurrentUser(principal);

        List<Transaction> transactions = transactionRepository.findFilteredTransactions(
                user.getId(), type, category, startDate, endDate
        );

        return ResponseEntity.ok(transactions);
    }

    @PostMapping
    public ResponseEntity<Transaction> create(@RequestBody Transaction transaction, Principal principal) {
        User user = getCurrentUser(principal);
        transaction.setUserId(user.getId());

        transaction.setSource("MANUAL");
        transaction.setExternalId(null);

        Transaction saved = transactionRepository.save(transaction);

        achievementService.checkAndUnlockAchievements(user.getId());

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id, Principal principal) {
        User user = getCurrentUser(principal);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Транзакцію не знайдено"));

        if (!transaction.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        transactionRepository.delete(transaction);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> updateTransaction(
            @PathVariable Long id,
            @RequestBody Transaction updatedData,
            Principal principal) {

        User user = getCurrentUser(principal);

        Transaction existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Транзакцію не знайдено"));

        if (!existingTransaction.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        if ("MONOBANK".equals(existingTransaction.getSource())) {
            existingTransaction.setCategory(updatedData.getCategory());
            existingTransaction.setDescription(updatedData.getDescription());
        } else {
            existingTransaction.setType(updatedData.getType());
            existingTransaction.setAmount(updatedData.getAmount());
            existingTransaction.setCategory(updatedData.getCategory());
            existingTransaction.setDescription(updatedData.getDescription());
            existingTransaction.setTransactionDate(updatedData.getTransactionDate());
            existingTransaction.setPaymentMethod(updatedData.getPaymentMethod());
        }

        return ResponseEntity.ok(transactionRepository.save(existingTransaction));
    }
}