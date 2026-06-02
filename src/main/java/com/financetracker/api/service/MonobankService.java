package com.financetracker.api.service;

import com.financetracker.api.model.Transaction;
import com.financetracker.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonobankService {

    private final TransactionRepository transactionRepository;
    private final WebClient.Builder webClientBuilder;

    public String syncTransactions(Long userId, String monoToken) {
        // 1. Отримуємо інформацію про клієнта та його рахунки
        String infoUrl = "https://api.monobank.ua/personal/client-info";
        Map<String, Object> clientInfo;

        try {
            clientInfo = webClientBuilder.build()
                    .get()
                    .uri(infoUrl)
                    .header("X-Token", monoToken)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                return "Помилка: Занадто багато запитів. Зачекайте 1 хвилину!";
            }
            return "Помилка авторизації в Монобанку: " + e.getMessage();
        } catch (Exception e) {
            return "Не вдалося отримати дані про рахунки: " + e.getMessage();
        }

        if (clientInfo == null || !clientInfo.containsKey("accounts")) {
            return "Рахунків у профілі Монобанку не знайдено.";
        }

        List<Map<String, Object>> accounts = (List<Map<String, Object>>) clientInfo.get("accounts");

        long timeTo = Instant.now().getEpochSecond();
        long timeFrom = Instant.now().minus(30, ChronoUnit.DAYS).getEpochSecond();

        int totalDownloaded = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> account : accounts) {
            String accountId = account.get("id").toString();

            if (account.containsKey("currencyCode") && account.get("currencyCode") == null) {
                continue;
            }

            String statementUrl = String.format("https://api.monobank.ua/personal/statement/%s/%d/%d", accountId, timeFrom, timeTo);

            try {
                List<Map<String, Object>> statements = webClientBuilder.build()
                        .get()
                        .uri(statementUrl)
                        .header("X-Token", monoToken)
                        .retrieve()
                        .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .collectList()
                        .block();

                if (statements != null && !statements.isEmpty()) {
                    for (Map<String, Object> stmt : statements) {
                        String bankId = stmt.get("id").toString();

                        if (transactionRepository.existsByExternalId(bankId)) {
                            continue;
                        }

                        Transaction tx = new Transaction();
                        tx.setUserId(userId);
                        tx.setExternalId(bankId);
                        tx.setSource("MONOBANK");

                        int amount = (Integer) stmt.get("amount");
                        tx.setAmount(BigDecimal.valueOf(Math.abs(amount / 100.0)));
                        tx.setType(amount > 0 ? "INCOME" : "EXPENSE");

                        tx.setCategory(stmt.get("description").toString());

                        long time = ((Number) stmt.get("time")).longValue();
                        LocalDate date = Instant.ofEpochSecond(time).atZone(ZoneId.systemDefault()).toLocalDate();
                        tx.setTransactionDate(date);

                        transactionRepository.save(tx);
                        totalDownloaded++;
                    }
                }

                Thread.sleep(500);

            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
                errors.add("Перевищено ліміт запитів при зчитуванні карток.");
            } catch (Exception e) {
                errors.add(accountId + ": " + e.getMessage());
            }
        }

        String responseMessage = "Синхронізація завершена. Додано нових транзакцій: " + totalDownloaded;
        if (!errors.isEmpty()) {
            responseMessage += " (Помилки з деякими рахунками: " + errors.size() + ")";
        }
        return responseMessage;
    }
}