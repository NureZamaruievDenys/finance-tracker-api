package com.financetracker.api.service;

import com.financetracker.api.model.Transaction;
import com.financetracker.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private final TransactionRepository transactionRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    public String generateResponse(Long userId, String userMessage) {
        List<Transaction> transactions = transactionRepository.findAllByUserId(userId);

        String txContext = transactions.stream()
                .limit(20)
                .map(t -> String.format("- %s: %.2f грн, категорія: %s, дата: %s",
                        t.getType(), t.getAmount(), t.getCategory(), t.getTransactionDate()))
                .collect(Collectors.joining("\n"));

        if (txContext.isEmpty()) {
            txContext = "Користувач ще не додав жодної транзакції.";
        }

        String systemPrompt = "Ти — професійний ШІ-асистент у додатку для обліку фінансів 'FinanceTracker'.\n" +
                "Твоє завдання — аналізувати витрати користувача та допомагати йому заощаджувати.\n" +
                "Ось останні фінансові дані користувача:\n" +
                txContext + "\n\n" +
                "Будь ласка, відповідай коротко, ємно, дружньо та виключно українською мовою. " +
                "Спирайся на надані вище дані.\n" +
                "ВАЖЛИВО: Після всіх своїх роздумів (Drafts) ОБОВ'ЯЗКОВО напиши чітку фінальну відповідь для користувача!";

        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> textPart = Map.of("text", userMessage);
        Map<String, Object> contentStructure = Map.of("parts", List.of(textPart));
        requestBody.put("contents", List.of(contentStructure));

        Map<String, Object> systemPart = Map.of("text", systemPrompt);
        Map<String, Object> systemStructure = Map.of("parts", List.of(systemPart));
        requestBody.put("systemInstruction", systemStructure);

        try {
            String finalUrl = apiUrl + "?key=" + apiKey.trim();

            Map<String, Object> response = webClientBuilder.build()
                    .post()
                    .uri(finalUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                    if (content != null && content.containsKey("parts")) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (!parts.isEmpty()) {
                            // Просто беремо весь текст як він є і віддаємо
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            }
        } catch (Exception e) {
            return "Системна помилка при роботі з ІІ: " + e.getMessage();
        }

        return "Не вдалося отримати відповідь від асистента.";
    }
}