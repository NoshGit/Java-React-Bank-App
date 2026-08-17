package com.example.bankbff.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Server-side proxy for the customer-support FAQ assistant.
 *
 * <p>The React widget POSTs the conversation here (via {@code /api/support/chat}), and this
 * controller forwards it to Google Gemini using a key held on the server. The key is therefore
 * never shipped to the browser — the recommended pattern for a banking application.</p>
 *
 * <p>This endpoint sits under {@code /api/**}, so the existing {@code SecurityConfig} already
 * requires an authenticated session to reach it.</p>
 *
 * <p>Configure in application.yml / properties:</p>
 * <pre>
 * support:
 *   chat:
 *     gemini:
 *       api-key: ${GEMINI_API_KEY:}        # inject from an environment variable / secret store
 *       model: gemini-2.5-flash            # override if the model id is retired
 * </pre>
 */
@RestController
@RequestMapping("/api/support")
public class SupportChatController {

    private static final String SYSTEM_PROMPT = """
            You are the Dynamic Bank Support Assistant, a friendly customer-support FAQ chatbot inside the Dynamic Bank web app.

            Scope: help customers and tellers use the app (sign-in, dashboards, transfers, paying someone, deposits,
            withdrawals, transaction history, reports, account status, fees) and answer general banking questions.

            Hard rules:
            - You have NO access to any customer's real accounts, balances, transactions, passwords, or OTPs. Never claim to.
            - Never ask for or accept full card numbers, PINs, passwords, OTPs, or CVV. Tell users to keep them private.
            - For account-specific or sensitive matters, direct users to the in-app Message Centre or Dynamic Bank support.
            - Do not give financial, legal, or investment advice.
            Keep answers short, clear, and in sentence case.
            """;

    private final RestClient restClient = RestClient.create();

    private final String apiKey;
    private final String model;

    public SupportChatController(
            @org.springframework.beans.factory.annotation.Value("${support.chat.gemini.api-key:}") String apiKey,
            @org.springframework.beans.factory.annotation.Value("${support.chat.gemini.model:gemini-2.5-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    /** One turn from the client conversation. role is "user" or "assistant". */
    public record ChatMessage(String role, String content) {
    }

    public record ChatRequest(List<ChatMessage> messages) {
    }

    public record ChatResponse(String reply) {
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chat(@RequestBody ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return new ChatResponse(
                    "The support assistant isn't fully configured yet. Please use the in-app Message Centre or contact Dynamic Bank support.");
        }
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return new ChatResponse("Ask me a question and I'll do my best to help.");
        }

        List<Map<String, Object>> contents = request.messages().stream()
                .map(m -> Map.of(
                        "role", "assistant".equals(m.role()) ? "model" : "user",
                        "parts", List.of(Map.of("text", m.content() == null ? "" : m.content()))))
                .toList();

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", contents,
                "generationConfig", Map.of("temperature", 0.4, "maxOutputTokens", 400));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String text = extractText(response);
            return new ChatResponse(text != null && !text.isBlank()
                    ? text
                    : "Sorry, I couldn't produce an answer just now. Please try again or use the Message Centre.");
        } catch (Exception e) {
            // Do not leak provider internals to the client.
            return new ChatResponse(
                    "Sorry, the assistant is temporarily unavailable. Please try again shortly, or contact Dynamic Bank support.");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object candidates = response.get("candidates");
        if (!(candidates instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> candidate)) {
            return null;
        }
        Object content = candidate.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) {
            return null;
        }
        Object parts = contentMap.get("parts");
        if (!(parts instanceof List<?> partsList) || partsList.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Object part : partsList) {
            if (part instanceof Map<?, ?> partMap) {
                Object text = partMap.get("text");
                if (text != null) {
                    sb.append(text);
                }
            }
        }
        return sb.toString();
    }
}
